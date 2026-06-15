package com.example.healtapp.features.social.presentation

import com.example.healtapp.data.network.dto.social.ClubPostResponseDto
import com.example.healtapp.data.network.dto.social.ClubResponseDto
import com.example.healtapp.data.network.dto.social.FeedPostDto

data class ClubFeedEntry(
    val club: ClubResponseDto,
    val post: ClubPostResponseDto,
)

sealed class CommunityTimelineItem {
    abstract val sortKey: String?

    data class UserPost(val post: FeedPostDto) : CommunityTimelineItem() {
        override val sortKey: String? = post.created_at
    }

    data class ClubPost(val club: ClubResponseDto, val post: ClubPostResponseDto) : CommunityTimelineItem() {
        override val sortKey: String? = post.created_at
    }
}

fun buildCommunityTimeline(
    userPosts: List<FeedPostDto>,
    clubEntries: List<ClubFeedEntry>,
): List<CommunityTimelineItem> {
    val items = userPosts.map { CommunityTimelineItem.UserPost(it) } +
        clubEntries.map { CommunityTimelineItem.ClubPost(it.club, it.post) }
    return items.sortedByDescending { it.sortKey.orEmpty() }
}
