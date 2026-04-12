package org.thoughtcrime.securesms.components.settings.conversation.preferences

import android.view.View
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.emoji.EmojiTextView
import org.thoughtcrime.securesms.components.settings.PreferenceModel
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.util.LongClickMovementMethod
import org.thoughtcrime.securesms.util.adapter.mapping.LayoutFactory
import org.thoughtcrime.securesms.util.adapter.mapping.MappingAdapter
import org.thoughtcrime.securesms.util.adapter.mapping.MappingViewHolder

object MessageCountPreference {

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.conversation_settings_message_count))
  }

  class Model(
    val threadId: Long
  ) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return threadId == newItem.threadId
    }

    override fun areContentsTheSame(newItem: Model): Boolean {
      return super.areContentsTheSame(newItem)
    }
  }

  class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val messageCountTextView: EmojiTextView = findViewById(R.id.message_count)

    override fun bind(model: Model) {
      messageCountTextView.movementMethod = LongClickMovementMethod.getInstance(context)

      // Includes update messages.
      val messageCount = SignalDatabase.messages.getMessageCountForThread(threadId = model.threadId)

      messageCountTextView.text = context.getString(R.string.preferences__s_messages, messageCount)
    }
  }
}
