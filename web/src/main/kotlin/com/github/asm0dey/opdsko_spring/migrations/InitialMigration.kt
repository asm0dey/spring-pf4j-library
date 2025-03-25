package com.github.asm0dey.opdsko_spring.migrations

import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoClient
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import io.mongock.driver.mongodb.reactive.util.MongoSubscriberSync
import io.mongock.driver.mongodb.reactive.util.SubscriberSync
import org.bson.Document


/**
 * Initial migration to ensure all books have the hasCover field.
 * This migration adds the hasCover field with a default value of true
 * to any book documents that don't already have this field.
 */
@ChangeUnit(id = "book-has-cover-migration", order = "1", author = "mongock")
class InitialMigration(val mongoClient: MongoClient) {

    @Execution
    fun execution() {
        val subscriber: SubscriberSync<UpdateResult> = MongoSubscriberSync()
        // Access the "books" collection from the "database" database
        val database = mongoClient.getDatabase("database")
        val collection = database.getCollection("books")

        // Find all books where hasCover field doesn't exist
        val query = Document("hasCover", Document("\$exists", false))

        // Update those books to set hasCover = true
        val update = Document("\$set", Document("hasCover", true))

        // Perform the update
        collection.updateMany(query, update).subscribe(subscriber)
        println("Migration completed: Updated ${subscriber.first.modifiedCount} books to have hasCover field")

    }

    @RollbackExecution
    fun rollbackExecution() {
        // Rollback is not needed for this migration as we're just adding a field with a default value
        // If needed, we could remove the field, but that's not recommended
        println("Rollback not implemented for this migration")
    }
}
