/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.conversation.shared

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.thoughtcrime.securesms.R

/** Centered secondary label showing how many messages are in this chat. */
@Composable
fun MessageCountLabel(
  messageCount: Int,
  modifier: Modifier = Modifier
) {
  Text(
    text = stringResource(R.string.ConversationSettingsFragment__s_messages, messageCount),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 4.dp)
  )
}
