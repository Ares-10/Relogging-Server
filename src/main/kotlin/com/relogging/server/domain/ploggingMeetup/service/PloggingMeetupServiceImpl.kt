package com.relogging.server.domain.ploggingMeetup.service

import com.relogging.server.domain.comment.entity.Comment
import com.relogging.server.domain.ploggingMeetup.dto.PloggingMeetupConverter
import com.relogging.server.domain.ploggingMeetup.dto.PloggingMeetupListResponse
import com.relogging.server.domain.ploggingMeetup.dto.PloggingMeetupRequest
import com.relogging.server.domain.ploggingMeetup.dto.PloggingMeetupResponse
import com.relogging.server.domain.ploggingMeetup.entity.PloggingMeetup
import com.relogging.server.domain.ploggingMeetup.repository.PloggingMeetupRepository
import com.relogging.server.global.exception.GlobalErrorCode
import com.relogging.server.global.exception.GlobalException
import com.relogging.server.infrastructure.aws.s3.AmazonS3Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class PloggingMeetupServiceImpl(
    private val ploggingMeetupRepository: PloggingMeetupRepository,
    private val amazonS3Service: AmazonS3Service,
    @Value("\${cloud.aws.s3.path.plogging-meetup}")
    private var imageUploadDir: String,
) : PloggingMeetupService {
    @Transactional
    override fun createMeetup(
        request: PloggingMeetupRequest,
        image: MultipartFile?,
    ): Long {
        val imageUrl = image?.let { amazonS3Service.uploadFile(it, imageUploadDir) }
        val ploggingMeetup = PloggingMeetupConverter.toEntity(request, imageUrl)
        return ploggingMeetupRepository.save(ploggingMeetup).id!!
    }

    @Transactional
    override fun getMeetup(
        id: Long,
        increaseHits: Boolean,
    ): PloggingMeetupResponse {
        val meetup =
            ploggingMeetupRepository.findById(id).orElseThrow {
                throw GlobalException(GlobalErrorCode.PLOGGING_MEETUP_NOT_FOUND)
            }
        return PloggingMeetupConverter.toResponse(meetup, getRootComments(meetup))
    }

    @Transactional
    override fun getNextMeetup(currentId: Long): PloggingMeetupResponse {
        val nextMeetup =
            ploggingMeetupRepository.findFirstByIdGreaterThanOrderByIdAsc(currentId)
                .orElseThrow {
                    throw GlobalException(GlobalErrorCode.PLOGGING_MEETUP_NOT_FOUND)
                }
        nextMeetup.hits += 1
        return PloggingMeetupConverter.toResponse(nextMeetup, getRootComments(nextMeetup))
    }

    @Transactional
    override fun getPrevMeetup(currentId: Long): PloggingMeetupResponse {
        val prevMeetup =
            ploggingMeetupRepository.findFirstByIdLessThanOrderByIdDesc(currentId)
                .orElseThrow {
                    throw GlobalException(GlobalErrorCode.PLOGGING_MEETUP_NOT_FOUND)
                }
        prevMeetup.hits += 1
        return PloggingMeetupConverter.toResponse(prevMeetup, getRootComments(prevMeetup))
    }

    @Transactional(readOnly = true)
    override fun getMeetupList(pageable: Pageable): PloggingMeetupListResponse {
        val response = ploggingMeetupRepository.findAll(pageable)
        return PloggingMeetupConverter.toResponse(response)
    }

    @Transactional
    override fun getMeetupEntity(id: Long): PloggingMeetup =
        ploggingMeetupRepository.findById(id).orElseThrow {
            throw GlobalException(GlobalErrorCode.PLOGGING_MEETUP_NOT_FOUND)
        }

    private fun getRootComments(meetup: PloggingMeetup): List<Comment> =
        meetup.commentList
            .filter { it.parentComment == null }
            .sortedByDescending { it.createAt }
}
