package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen

/**
 * Filter option for message sender: All, Customer, or AI Agent.
 */
enum class SenderFilter(val label: String) {
    ALL("All"),
    CUSTOMER("Customer"),
    AI("Zama AI")
}

/**
 * Filter option for triage priority: All, Urgent, or General.
 */
enum class PriorityFilter(val label: String, val tag: String) {
    ALL("All", "ALL"),
    URGENT("Urgent", "URGENT"),
    GENERAL("General", "GENERAL")
}

/**
 * Search and Filter Bar at the top of the chat history screen.
 * Allows instant filtering of messages by content keywords, sender type, or triage priority.
 */
@Composable
fun ChatSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSender: SenderFilter,
    onSenderSelected: (SenderFilter) -> Unit,
    totalCount: Int,
    filteredCount: Int,
    customerCount: Int,
    aiCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    selectedPriority: PriorityFilter = PriorityFilter.ALL,
    onPrioritySelected: ((PriorityFilter) -> Unit)? = null,
    urgentCount: Int = 0,
    generalCount: Int = 0,
    onExportClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isFilteringActive = searchQuery.isNotBlank() ||
            selectedSender != SenderFilter.ALL ||
            selectedPriority != PriorityFilter.ALL

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B1017))
            .border(BorderStroke(0.5.dp, Color(0x3300E5FF)))
            .testTag("chat_search_bar")
    ) {
        // Main Search Bar Header / Toggle Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stylized Search Input Box
            Surface(
                color = Color(0xFF131A24),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (searchQuery.isNotEmpty()) ZamaElectricCyan else Color(0x338696A0)
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = if (searchQuery.isNotEmpty()) ZamaElectricCyan else ZamaChromeMid,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search messages by content or keyword...",
                                color = ZamaChromeMid,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            textStyle = TextStyle(
                                color = ZamaChromeLight,
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.SansSerif
                            ),
                            cursorBrush = SolidColor(ZamaElectricCyan),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_search_input")
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onSearchQueryChange("")
                                focusManager.clearFocus()
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("chat_search_clear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search query",
                                tint = ZamaChromeMid,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Quick Filter Expander / Status Chip
            Surface(
                color = if (isFilteringActive) Color(0x2600E5FF) else Color(0xFF131A24),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isFilteringActive) ZamaElectricCyan else Color(0x338696A0)
                ),
                modifier = Modifier
                    .clickable { onToggleExpand() }
                    .heightIn(min = 44.dp)
                    .testTag("chat_search_expand_filters_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Sender filter toggle",
                        tint = if (isFilteringActive) ZamaElectricCyan else ZamaChromeMid,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = selectedSender.label,
                        color = if (isFilteringActive) ZamaElectricCyan else ZamaChromeLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Expandable Sender Filter Chips & Match Count Bar
        AnimatedVisibility(
            visible = isExpanded || isFilteringActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
            ) {
                // Sender Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FILTER SENDER:",
                        color = ZamaChromeMid,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    // "All" Sender Chip
                    SenderFilterChip(
                        label = "All ($totalCount)",
                        isSelected = selectedSender == SenderFilter.ALL,
                        icon = null,
                        color = ZamaElectricCyan,
                        onClick = { onSenderSelected(SenderFilter.ALL) },
                        tag = "sender_filter_all"
                    )

                    // "Customer" Sender Chip
                    SenderFilterChip(
                        label = "Customer ($customerCount)",
                        isSelected = selectedSender == SenderFilter.CUSTOMER,
                        icon = Icons.Default.Person,
                        color = ZamaNeonGreen,
                        onClick = { onSenderSelected(SenderFilter.CUSTOMER) },
                        tag = "sender_filter_customer"
                    )

                    // "Zama AI" Sender Chip
                    SenderFilterChip(
                        label = "Zama AI ($aiCount)",
                        isSelected = selectedSender == SenderFilter.AI,
                        icon = Icons.Default.SmartToy,
                        color = ZamaElectricCyan,
                        onClick = { onSenderSelected(SenderFilter.AI) },
                        tag = "sender_filter_ai"
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Triage Priority Filter Chips Row (High-visibility distinction for Urgent vs General)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRIAGE PRIORITY:",
                        color = ZamaChromeMid,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    // "All" Priority Chip
                    SenderFilterChip(
                        label = "All ($totalCount)",
                        isSelected = selectedPriority == PriorityFilter.ALL,
                        icon = null,
                        color = ZamaElectricCyan,
                        onClick = { onPrioritySelected?.invoke(PriorityFilter.ALL) },
                        tag = "priority_filter_all"
                    )

                    // "Urgent" Priority Chip (high-visibility bright red/crimson)
                    SenderFilterChip(
                        label = "🚨 Urgent ($urgentCount)",
                        isSelected = selectedPriority == PriorityFilter.URGENT,
                        icon = null,
                        color = Color(0xFFFF1744),
                        onClick = { onPrioritySelected?.invoke(PriorityFilter.URGENT) },
                        tag = "priority_filter_urgent"
                    )

                    // "General" Priority Chip (high-visibility cyan/emerald)
                    SenderFilterChip(
                        label = "💬 General ($generalCount)",
                        isSelected = selectedPriority == PriorityFilter.GENERAL,
                        icon = null,
                        color = Color(0xFF00E5FF),
                        onClick = { onPrioritySelected?.invoke(PriorityFilter.GENERAL) },
                        tag = "priority_filter_general"
                    )
                }

                // Match Statistics and Clear Action
                if (isFilteringActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (filteredCount > 0) {
                                "Showing $filteredCount of $totalCount messages"
                            } else {
                                "No matches found"
                            },
                            color = if (filteredCount > 0) ZamaElectricCyan else Color(0xFFFF5252),
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onExportClick != null && filteredCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { onExportClick() }
                                        .padding(4.dp)
                                        .testTag("search_export_results_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Export results",
                                        tint = ZamaElectricCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "EXPORT",
                                        color = ZamaElectricCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        onSearchQueryChange("")
                                        onSenderSelected(SenderFilter.ALL)
                                        onPrioritySelected?.invoke(PriorityFilter.ALL)
                                        focusManager.clearFocus()
                                    }
                                    .padding(4.dp)
                                    .testTag("reset_search_filters_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset filters",
                                    tint = ZamaElectricCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "RESET FILTERS",
                                    color = ZamaElectricCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter Chip for Message Sender.
 */
@Composable
private fun SenderFilterChip(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    color: Color,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.2f) else Color(0xFF141C27),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) color else Color(0x338696A0)
        ),
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clickable { onClick() }
            .testTag(tag)
            .semantics { contentDescription = "Filter by $label" }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) color else ZamaChromeMid,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                color = if (isSelected) Color.White else ZamaChromeLight,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Empty search results placeholder shown when search query or filter returns no messages.
 */
@Composable
fun EmptySearchStatePlaceholder(
    query: String,
    senderFilter: SenderFilter,
    priorityFilter: PriorityFilter = PriorityFilter.ALL,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B2433)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = "No results found",
                tint = ZamaElectricCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "No messages found",
            color = ZamaChromeLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )

        Spacer(modifier = Modifier.height(4.dp))

        val filterDesc = buildString {
            if (query.isNotBlank()) append("\"$query\" ")
            if (senderFilter != SenderFilter.ALL) append("(${senderFilter.label}) ")
            if (priorityFilter != PriorityFilter.ALL) append("[${priorityFilter.label}] ")
        }.trim()

        Text(
            text = if (filterDesc.isNotBlank()) {
                "No messages match $filterDesc."
            } else {
                "No messages found."
            },
            color = ZamaChromeMid,
            fontSize = 11.5.sp,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            color = Color(0x2600E5FF),
            shape = RoundedCornerShape(100.dp),
            border = BorderStroke(1.dp, ZamaElectricCyan),
            modifier = Modifier
                .clickable { onResetFilters() }
                .testTag("empty_search_reset_btn")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = ZamaElectricCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Clear Search & Show All",
                    color = ZamaElectricCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
