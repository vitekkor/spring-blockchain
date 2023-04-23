package com.vitekkor.blockchain.stub

import com.vitekkor.blockchain.model.HttpIncomingMessage
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/stub")
class NodeStub(private val nodeStubDelegate: NodeStubDelegate) {
    @GetMapping("/lastBlock")
    fun getLastStubBlock(httpServletRequest: HttpServletRequest): HttpOutgoingMessage.LastBlockMessage {
        val lastBlock = nodeStubDelegate.getLastBlock()
        return HttpOutgoingMessage.LastBlockMessage(lastBlock)
    }

    @GetMapping("/blockChain")
    fun getStubBlockChain(httpServletRequest: HttpServletRequest): HttpOutgoingMessage.BlockChainMessage {
        val blocks = nodeStubDelegate.getBlockChain()
        return HttpOutgoingMessage.BlockChainMessage(blocks)
    }

    @PostMapping("/newBlock")
    fun postNewStubBlock(
        httpServletRequest: HttpServletRequest,
        @RequestBody newBlock: HttpIncomingMessage.NewBlockMessage
    ): HttpOutgoingMessage {
        return nodeStubDelegate.validateNewBlockAndSave(newBlock.block)
    }
}
