package com.example.healtapp.features.timeline.ui

import androidx.compose.runtime.Composable
import com.example.healtapp.features.social.ui.CommunityFeedScreen

/** Раньше «Лента здоровья» — теперь лента сообщества. */
@Composable
fun TimelineScreen(
    onBack: () -> Unit = {},
    onOpenFriend: (Int) -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenClub: (Int) -> Unit = {},
) {
    CommunityFeedScreen(
        onBack = onBack,
        onOpenFriend = onOpenFriend,
        onOpenFriends = onOpenFriends,
        onOpenClub = onOpenClub,
    )
}
