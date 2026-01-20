package org.thoughtcrime.securesms.util

import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.database.model.MessageRecord
import org.thoughtcrime.securesms.groups.GroupId
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import java.util.Optional
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Helpers for determining if a message send/receive is valid for those that
 * have strict time limits.
 */
object MessageConstraintsUtil {
  private val RECEIVE_THRESHOLD = TimeUnit.DAYS.toMillis(2)
  private val SEND_THRESHOLD = TimeUnit.DAYS.toMillis(1)

  const val MAX_EDIT_COUNT = 10

  private val TAG = Log.tag(MessageConstraintsUtil.javaClass)
  @JvmStatic
  fun isValidRemoteDeleteReceive(targetMessage: MessageRecord, deleteSender: Recipient, deleteServerTimestamp: Long): Boolean {
    val selfIsDeleteSender = isSelf(deleteSender.id)

    val targetMessageGroupId: Optional<GroupId> = targetMessage.toRecipient.groupId
    val deleteSentByGroupAdmin: Boolean = if (targetMessageGroupId.isPresent) SignalDatabase.groups.getGroup(targetMessageGroupId.get()).get().admins.contains(deleteSender) else false
    Log.d(TAG, "delete sent by group admin: $deleteSentByGroupAdmin")

    Log.d(TAG, "selfIsDeleteSender: $selfIsDeleteSender, isOutgoing: ${targetMessage.isOutgoing}")
    val isValidIncomingOutgoing = selfIsDeleteSender && targetMessage.isOutgoing || !selfIsDeleteSender && !targetMessage.isOutgoing || deleteSentByGroupAdmin && targetMessage.isOutgoing
    val isValidSender = targetMessage.fromRecipient.id == deleteSender.id || selfIsDeleteSender && targetMessage.isOutgoing || deleteSentByGroupAdmin

    val messageTimestamp = if (selfIsDeleteSender && targetMessage.isOutgoing) targetMessage.dateSent else targetMessage.serverTimestamp

    val bool = (deleteServerTimestamp - messageTimestamp < RECEIVE_THRESHOLD) || (selfIsDeleteSender && targetMessage.isOutgoing) || (!selfIsDeleteSender && deleteSentByGroupAdmin)
    Log.d(TAG, "valid I/O: $isValidIncomingOutgoing, valid sender: $isValidSender, other: $bool")
    return isValidIncomingOutgoing && isValidSender && bool
  }

  @JvmStatic
  fun isValidEditMessageReceive(targetMessage: MessageRecord, editSender: Recipient, editServerTimestamp: Long): Boolean {
    return isValidRemoteDeleteReceive(targetMessage, editSender, editServerTimestamp)
  }

  @JvmStatic
  fun isValidRemoteDeleteSend(targetMessages: Collection<MessageRecord>, currentTime: Long): Boolean {
    // TODO [greyson] [remote-delete] Update with server timestamp when available for outgoing messages
    return targetMessages.all { isValidRemoteDeleteSend(it, currentTime) }
  }

  @JvmStatic
  fun isWithinMaxEdits(targetMessage: MessageRecord): Boolean {
    return targetMessage.revisionNumber < MAX_EDIT_COUNT
  }

  @JvmStatic
  fun getEditMessageThresholdHours(): Int {
    return SEND_THRESHOLD.milliseconds.inWholeHours.toInt()
  }

  /**
   * Check if at the current time a target message can be edited
   */
  @JvmStatic
  fun isValidEditMessageSend(targetMessage: MessageRecord, currentTime: Long): Boolean {
    val originalMessage = if (targetMessage.isEditMessage && targetMessage.id != targetMessage.originalMessageId?.id) {
      SignalDatabase.messages.getMessageRecord(targetMessage.originalMessageId!!.id)
    } else {
      targetMessage
    }

    val isNoteToSelf = targetMessage.toRecipient.isSelf && targetMessage.fromRecipient.isSelf

    return isValidRemoteDeleteSend(originalMessage, currentTime) &&
      (isNoteToSelf || targetMessage.revisionNumber < MAX_EDIT_COUNT) &&
      !targetMessage.isViewOnceMessage() &&
      !targetMessage.hasAudio() &&
      !targetMessage.hasSharedContact() &&
      !targetMessage.hasSticker() &&
      !targetMessage.hasPoll()
  }

  /**
   * Check regardless of timing, whether a target message can be edited
   */
  @JvmStatic
  fun isValidEditMessageSend(targetMessage: MessageRecord): Boolean {
    return isValidEditMessageSend(targetMessage, targetMessage.dateSent)
  }

  private fun isValidRemoteDeleteSend(message: MessageRecord, currentTime: Long): Boolean {
    return !message.isUpdate &&
      message.isOutgoing &&
      message.isPush &&
      (!message.toRecipient.isGroup || message.toRecipient.isActiveGroup) &&
      !message.isRemoteDelete &&
      !message.hasGiftBadge() &&
      !message.isPaymentNotification &&
      !message.isPaymentTombstone &&
      (currentTime - message.dateSent < SEND_THRESHOLD || message.toRecipient.isSelf)
  }

  private fun isSelf(recipientId: RecipientId): Boolean {
    return Recipient.isSelfSet && Recipient.self().id == recipientId
  }
}
