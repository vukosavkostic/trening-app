package com.vukos.trening

import com.vukos.trening.notion.NotionClient
import com.vukos.trening.notion.PlanParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Proverava pravi Notion (samo citanje). Pokrece se samo lokalno, kad postoji
 * NOTION_TOKEN_FILE; token nikad nije u repozitorijumu.
 */
class NotionLiveTest {
    private val tokenFile = System.getenv("NOTION_TOKEN_FILE")?.let(::File)

    @Test fun discoverAndReadExampleRoutine() = runBlocking {
        assumeTrue(tokenFile?.exists() == true)
        val c = NotionClient(tokenFile!!.readText().trim())
        val dbs = c.discoverDatabases()
        assertEquals(setOf("Exercises", "Routines", "Workouts", "Sets", "Body", "Food"), dbs.keys)
        val pages = c.queryDatabase(dbs.getValue("Routines"))
        val example = pages.first { NotionClient.titleOf(it) == "Primer - Push" }
        assertEquals(listOf("Pon", "Čet"), NotionClient.multiSelect(example, "Days"))
        val lines = PlanParser.parseBlocks(c.children(example.getString("id")).map(NotionClient::blockText))
        assertEquals(4, lines.size)
        assertTrue(lines.last().kg == 25.0)
    }
}
