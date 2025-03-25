package com.github.asm0dey.opdsko_spring.migrations

import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoClient
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import io.mongock.driver.mongodb.reactive.util.MongoSubscriberSync
import io.mongock.driver.mongodb.reactive.util.SubscriberSync
import org.bson.Document

@ChangeUnit(id = "author-full-name-migration", order = "2", author = "mongock")
class AuthorFullNameMigration(val mongoClient: MongoClient) {

    @Execution
    fun execution() {
        val subscriber: SubscriberSync<UpdateResult> = MongoSubscriberSync()
        val database = mongoClient.getDatabase("database")
        val collection = database.getCollection("books")

        val query = Document()
        val update = listOf(
            Document(
                "\$set", Document(
                    "authors", Document(
                        "\$map", Document()
                            .append("input", "\$authors")
                            .append("as", "author")
                            .append(
                                "in", Document(
                                    "\$cond", Document()
                                        .append("if", Document("\$ifNull", listOf("\$\$author.fullName", false)))
                                        .append("then", "\$\$author") // Keep existing document if fullName exists
                                        .append(
                                            "else", Document()
                                                .append("lastName", "\$\$author.lastName")
                                                .append("middleName", "\$\$author.middleName")
                                                .append("nickname", "\$\$author.nickname")
                                                .append("firstName", "\$\$author.firstName")
                                                .append(
                                                    "fullName",
                                                    Document(
                                                        "\$concat",
                                                        listOf("\$\$author.lastName", ", ", "\$\$author.firstName")
                                                    )
                                                )
                                        )
                                )
                            )
                    )
                )
            )
        )

        collection.updateMany(query, update).subscribe(subscriber)
        println("Migration completed: Updated ${subscriber.first.modifiedCount} books to add fullName field to authors")
    }

    @RollbackExecution
    fun rollbackExecution() {
        val subscriber: SubscriberSync<UpdateResult> = MongoSubscriberSync()
        val database = mongoClient.getDatabase("database")
        val collection = database.getCollection("books")

        val query = Document()
        val update = listOf(
            Document(
                "\$set", Document(
                    "authors", Document(
                        "\$map", Document()
                            .append("input", "\$authors")
                            .append("as", "author")
                            .append(
                                "in", Document()
                                    .append("lastName", "\$\$author.lastName")
                                    .append("firstName", "\$\$author.firstName")
                            )
                    )
                )
            )
        )

        collection.updateMany(query, update).subscribe(subscriber)
        println("Rollback completed: Removed fullName field from authors in ${subscriber.first.modifiedCount} books")
    }
}