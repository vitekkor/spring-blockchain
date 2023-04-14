package com.vitekkor.blockchain.controller

import com.vitekkor.blockchain.model.HttpIncomingMessage
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import com.vitekkor.blockchain.service.BlockGeneratorService
import mu.KotlinLogging.logger
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
class BlockchainController(private val blockGeneratorService: BlockGeneratorService) {
    private val log = logger {}

    @GetMapping("/start")
    fun start() {
        log.info("Start node...")
        blockGeneratorService.start()
        log.info("Node started successfully")
    }

    @GetMapping("/stop")
    fun stop() {
        log.info("Stop node...")
        blockGeneratorService.stop()
        log.info("Node stopped successfully")
    }

    @GetMapping("/lastBlock")
    fun getLastBlock(httpServletRequest: HttpServletRequest): HttpOutgoingMessage.LastBlockMessage {
        log.info("Incoming get last block request from ${httpServletRequest.remoteAddr}")
        val lastBlock = blockGeneratorService.getLastBlock()
        return HttpOutgoingMessage.LastBlockMessage(lastBlock)
    }

    @GetMapping("/blockChain")
    fun getBlockChain(httpServletRequest: HttpServletRequest): HttpOutgoingMessage.BlockChainMessage {
        log.info("Incoming get block chain request from ${httpServletRequest.remoteAddr}")
        val blocks = blockGeneratorService.getBlockChain()
        return HttpOutgoingMessage.BlockChainMessage(blocks)
    }

    @PostMapping("/newBlock")
    fun postNewBlock(
        httpServletRequest: HttpServletRequest,
        @RequestBody newBlock: HttpIncomingMessage.NewBlockMessage
    ): HttpOutgoingMessage {
        log.info("Incoming post new block request from ${httpServletRequest.remoteAddr}")
        return blockGeneratorService.validateNewBlockAndSave(newBlock.block)
    }

    @GetMapping("/generateGenesys")
    fun generateGenesis() {
        log.info("Generating genesys...")
        blockGeneratorService.generateGenesis()
        log.info("Genesys generated successfully")
    }
}
