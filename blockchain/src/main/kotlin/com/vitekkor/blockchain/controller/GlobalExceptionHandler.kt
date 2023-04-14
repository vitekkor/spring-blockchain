package com.vitekkor.blockchain.controller

import com.vitekkor.blockchain.exception.NoBlocksInNodeException
import mu.KotlinLogging.logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class GlobalExceptionHandler {

    private val log = logger {}

    @ExceptionHandler(NoBlocksInNodeException::class)
    fun handleNoBlocksInNodeException(
        e: NoBlocksInNodeException,
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<String> {
        log.error { "Request for lastBlock from ${httpServletRequest.remoteAddr}: Node has no blocks!" }
        return ResponseEntity.badRequest().body("Node has no blocks!")
    }
}
