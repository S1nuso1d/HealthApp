package com.example.healtapp.domain.usecase.profile

import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.example.healtapp.domain.repository.ProfileRepository
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(): Result<ProfileDto> {
        return profileRepository.getMyProfile()
    }
}
