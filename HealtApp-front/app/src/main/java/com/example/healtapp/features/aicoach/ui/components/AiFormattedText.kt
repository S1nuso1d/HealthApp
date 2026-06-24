package com.example.healtapp.features.aicoach.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.contentPrimaryColor

private sealed interface AiMessageBlock {
    data class Heading(val level: Int, val text: String) : AiMessageBlock
    data class Paragraph(val text: String) : AiMessageBlock
    data class Bullet(val text: String) : AiMessageBlock
    data class Numbered(val index: String, val text: String) : AiMessageBlock
}

private val headingRegex = Regex("""^(#{1,4})\s*(.+)$""")
private val bulletRegex = Regex("""^[-*•]\s+(.+)$""")
private val numberedRegex = Regex("""^(\d+)[.)]\s+(.+)$""")
private val fenceRegex = Regex("""^```.*$""")

private fun sanitizeAiMarkdown(raw: String): String {
    return raw
        .replace(Regex("""```\w*\n?"""), "")
        .replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "")
        .replace(Regex("""^#+\s*$""", RegexOption.MULTILINE), "")
        .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
        .replace(Regex("""__(.+?)__"""), "$1")
        .replace(Regex("""\*{2,}"""), "")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

private fun parseAiMessageBlocks(raw: String): List<AiMessageBlock> {
        val normalized = sanitizeAiMarkdown(raw)
            .replace("\r\n", "\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()

        if (normalized.isEmpty()) return emptyList()

        val blocks = mutableListOf<AiMessageBlock>()
        val paragraph = StringBuilder()
        var inFence = false

        fun flushParagraph() {
            val text = paragraph.toString().trim()
            if (text.isNotEmpty()) {
                blocks.add(AiMessageBlock.Paragraph(text))
            }
            paragraph.clear()
        }

        normalized.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> flushParagraph()
                fenceRegex.matches(trimmed) -> {
                    flushParagraph()
                    inFence = !inFence
                }
                inFence -> {
                    if (paragraph.isNotEmpty()) paragraph.append('\n')
                    paragraph.append(trimmed)
                }
                headingRegex.matches(trimmed) -> {
                    flushParagraph()
                    val match = headingRegex.matchEntire(trimmed) ?: return@forEach
                    blocks.add(
                        AiMessageBlock.Heading(
                            level = match.groupValues[1].length,
                            text = match.groupValues[2].trim(),
                        ),
                    )
                }
                bulletRegex.matches(trimmed) -> {
                    flushParagraph()
                    val match = bulletRegex.matchEntire(trimmed) ?: return@forEach
                    blocks.add(AiMessageBlock.Bullet(match.groupValues[1].trim()))
                }
                numberedRegex.matches(trimmed) -> {
                    flushParagraph()
                    val match = numberedRegex.matchEntire(trimmed) ?: return@forEach
                    blocks.add(
                        AiMessageBlock.Numbered(
                            index = match.groupValues[1],
                            text = match.groupValues[2].trim(),
                        ),
                    )
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(trimmed)
                }
            }
        }
        flushParagraph()
        return blocks
}

object AiMessageFormatter {
    fun annotateInline(text: String): AnnotatedString = buildAnnotatedString {
        val pattern = Regex("""\*\*(.+?)\*\*|__(.+?)__|\*(.+?)\*""")
        var cursor = 0
        pattern.findAll(text).forEach { match ->
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }
            val bold = match.groups[1]?.value ?: match.groups[2]?.value
            val italic = match.groups[3]?.value
            when {
                bold != null -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(bold) }
                italic != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

@Composable
fun AiFormattedText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { parseAiMessageBlocks(text) }
    if (blocks.isEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = contentPrimaryColor(),
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is AiMessageBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium
                        2 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.labelLarge
                    }
                    Text(
                        text = AiMessageFormatter.annotateInline(block.text),
                        style = style,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                    )
                }
                is AiMessageBlock.Paragraph -> {
                    Text(
                        text = AiMessageFormatter.annotateInline(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentPrimaryColor(),
                    )
                }
                is AiMessageBlock.Bullet -> {
                    Row {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = AiMessageFormatter.annotateInline(block.text),
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentPrimaryColor(),
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
                is AiMessageBlock.Numbered -> {
                    Row {
                        Text(
                            text = "${block.index}.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = AiMessageFormatter.annotateInline(block.text),
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentPrimaryColor(),
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
    }
}
