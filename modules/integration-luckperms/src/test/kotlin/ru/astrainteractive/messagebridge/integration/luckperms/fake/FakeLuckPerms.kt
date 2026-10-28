@file:Suppress("ArgumentPerLine", "ExpressionBodyOnOneLine", "FunctionVerb", "TrivialLambdaReference")

package ru.astrainteractive.messagebridge.integration.luckperms.fake

import net.luckperms.api.LuckPerms
import net.luckperms.api.model.data.DataMutateResult
import net.luckperms.api.model.data.NodeMap
import net.luckperms.api.model.user.User
import net.luckperms.api.model.user.UserManager
import net.luckperms.api.node.Node
import net.luckperms.api.node.NodeBuilderRegistry
import net.luckperms.api.node.types.InheritanceNode
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

internal class FakeLuckPerms(
    private val saveFailure: Throwable?
) {
    private val groupsByUser = mutableMapOf<UUID, MutableSet<String>>()

    private val nodesByGroup = mutableMapOf<String, InheritanceNode>()

    private val userManager: UserManager = proxy { method, args ->
        when (method.name) {
            "modifyUser" -> modifyUser(args[0] as UUID, args[1])
            else -> unsupported(method)
        }
    }

    private val nodeBuilderRegistry: NodeBuilderRegistry = proxy { method, _ ->
        when (method.name) {
            "forInheritance" -> groupBuilder()
            else -> unsupported(method)
        }
    }

    val luckPerms: LuckPerms = proxy { method, _ ->
        when (method.name) {
            "getUserManager" -> userManager
            "getNodeBuilderRegistry" -> nodeBuilderRegistry
            else -> unsupported(method)
        }
    }

    var mutationCount = 0
        private set

    private inline fun <reified T : Any> proxy(crossinline answer: (Method, List<Any?>) -> Any?): T {
        val handler = { _: Any, method: Method, args: Array<Any?>? ->
            answer.invoke(method, args?.toList().orEmpty())
        }
        return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java), handler) as T
    }

    private fun unsupported(method: Method): Nothing {
        throw UnsupportedOperationException("FakeLuckPerms does not answer ${method.name}")
    }

    private fun groupNode(group: String): InheritanceNode = nodesByGroup.getOrPut(group) {
        proxy { method, args ->
            when (method.name) {
                "getGroupName" -> group
                "equals" -> (args.single() as? InheritanceNode)?.groupName == group
                "hashCode" -> group.hashCode()
                "toString" -> "InheritanceNode($group)"
                else -> unsupported(method)
            }
        }
    }

    private fun groupBuilder(): InheritanceNode.Builder {
        var group = ""
        lateinit var builder: InheritanceNode.Builder
        builder = proxy { method, args ->
            when (method.name) {
                "group" -> builder.also { group = args.single() as String }
                "build" -> groupNode(group)
                else -> unsupported(method)
            }
        }
        return builder
    }

    private fun mutate(uuid: UUID, node: Any?, change: (MutableSet<String>, String) -> Unit): DataMutateResult {
        val inheritanceNode = node as InheritanceNode
        mutationCount++
        change.invoke(groupsByUser.getOrPut(uuid) { mutableSetOf() }, inheritanceNode.groupName)
        return DataMutateResult.SUCCESS
    }

    private fun nodeMap(uuid: UUID): NodeMap = proxy { method, args ->
        when (method.name) {
            "add" -> mutate(uuid, args.single()) { groups, group -> groups.add(group) }
            "remove" -> mutate(uuid, args.single()) { groups, group -> groups.remove(group) }
            else -> unsupported(method)
        }
    }

    fun groupsOf(uuid: UUID): Set<String> = groupsByUser[uuid].orEmpty().toSet()

    private fun nodesOf(uuid: UUID): List<Node> = groupsOf(uuid).map { group -> groupNode(group) }

    private fun user(uuid: UUID): User = proxy { method, args ->
        when {
            method.name == "getNodes" && args.isEmpty() -> nodesOf(uuid)
            method.name == "data" -> nodeMap(uuid)
            else -> unsupported(method)
        }
    }

    private fun modifyUser(uuid: UUID, action: Any?): CompletableFuture<Void> {
        Consumer::class.java.getMethod("accept", Any::class.java).invoke(action, user(uuid))
        if (saveFailure != null) {
            return CompletableFuture.failedFuture(saveFailure)
        }
        return CompletableFuture.completedFuture(null)
    }

    fun addGroup(uuid: UUID, group: String) {
        groupsByUser.getOrPut(uuid) { mutableSetOf() }.add(group)
    }
}
