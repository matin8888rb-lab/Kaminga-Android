package com.kaminga.reader

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF090D1A)
private val Panel = Color(0xFF151B2E)
private val PanelHigh = Color(0xFF1D2540)
private val Purple = Color(0xFF9B7BFF)
private val Cyan = Color(0xFF64E5E8)
private val Muted = Color(0xFFADB5CC)
private val Line = Color(0xFF343B54)

private data class Comic(val title: String, val english: String, val genre: String, val description: String, val symbol: String, val accent: Color, val chapters: Int)
private val comics = listOf(
    Comic("سقوط ستاره‌ها", "STARFALL", "علمی‌تخیلی", "وقتی آخرین ستاره خاموش می‌شود، یک پیام ناشناس مسیر سرنوشت بشر را تغییر می‌دهد.", "✦", Purple, 12),
    Comic("نگهبان شب", "NIGHTWARD", "ابرقهرمانی", "قهرمانی تازه‌کار باید میان قدرت و مسئولیت انتخاب کند.", "◈", Cyan, 8),
    Comic("شهر بی‌صدا", "SILENT CITY", "معمایی", "در شهری خاموش، یک دانش‌آموز رازی قدیمی را پیدا می‌کند.", "⌁", Color(0xFFFFB86B), 16),
    Comic("آخرین سامورایی", "LAST SAMURAI", "مانگا", "ماجرای شمشیرزنی جوان در سفری برای یافتن حقیقت خانواده‌اش.", "刀", Color(0xFFFF8A9B), 21),
    Comic("ماه شیشه‌ای", "GLASS MOON", "مانهوا", "دو جهان موازی با انتخابی غیرمنتظره به هم گره می‌خورند.", "☾", Color(0xFF8CA8FF), 10)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KamingaApp() }
    }
}

@Composable
private fun KamingaApp() {
    var tab by rememberSaveable { mutableStateOf("خانه") }
    var query by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("همه") }
    var selected by remember { mutableStateOf<Comic?>(null) }
    var reading by remember { mutableStateOf<Comic?>(null) }
    var readingChapter by rememberSaveable { mutableIntStateOf(1) }
    var mode by rememberSaveable { mutableStateOf("صفحه‌ای") }
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("kaminga_local", Context.MODE_PRIVATE) }
    val saved = remember {
        mutableStateListOf<String>().apply {
            addAll(preferences.getStringSet("saved_comics", emptySet())?.toList() ?: emptyList())
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, secondary = Cyan, background = Night, surface = Panel, onPrimary = Night, onBackground = Color.White, onSurface = Color.White)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = Night) {
                when {
                    reading != null -> ReaderScreen(reading!!, readingChapter, mode, { mode = it }, { readingChapter = (readingChapter - 1).coerceAtLeast(1) }, { readingChapter = (readingChapter + 1).coerceAtMost(reading!!.chapters) }, { reading = null })
                    selected != null -> DetailScreen(selected!!, saved.contains(selected!!.title), {
                        if (saved.contains(selected!!.title)) saved.remove(selected!!.title) else saved.add(selected!!.title)
                        preferences.edit().putStringSet("saved_comics", saved.toSet()).apply()
                    }, { chapter -> readingChapter = chapter; reading = selected }, { selected = null })
                    else -> Scaffold(containerColor = Night, bottomBar = {
                        NavigationBar(containerColor = Panel) {
                            listOf("خانه", "جست‌وجو", "کتابخانه", "حساب").forEach { label ->
                                val icon = when(label) {
                                    "خانه" -> Icons.Default.Home
                                    "جست‌وجو" -> Icons.Default.Search
                                    "کتابخانه" -> Icons.Default.Bookmarks
                                    else -> Icons.Default.Person
                                }
                                NavigationBarItem(selected = tab == label, onClick = { tab = label }, icon = { Icon(icon, null) }, label = { Text(label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Cyan, selectedTextColor = Cyan, indicatorColor = PanelHigh, unselectedIconColor = Muted, unselectedTextColor = Muted))
                            }
                        }
                    }) { padding ->
                        when(tab) {
                            "جست‌وجو" -> SearchScreen(query, { query = it }, genre, { genre = it }, comics.filter { (it.title + it.english + it.genre).contains(query, true) && (genre == "همه" || it.genre == genre) }, { selected = it }, Modifier.padding(padding))
                            "کتابخانه" -> LibraryScreen(comics.filter { saved.contains(it.title) }, { selected = it }, Modifier.padding(padding))
                            "حساب" -> AccountScreen(Modifier.padding(padding))
                            else -> HomeScreen(saved.size, { tab = "جست‌وجو" }, { selected = it }, Modifier.padding(padding))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(savedCount: Int, onSearch: () -> Unit, onOpen: (Comic) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("KAMINGA", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("داستانی برای هر سلیقه", color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = onSearch, modifier = Modifier.background(Panel, CircleShape)) {
                    Icon(Icons.Default.Search, contentDescription = "جست‌وجو", tint = Color.White)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Text("دنیای داستان‌ها", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("کمیک، مانگا و مانهوا را کشف کن.", color = Muted, fontSize = 13.sp)
            }
        }
        item {
            Column {
                Heading("پیشنهاد کامینگا", "منتخب")
                Spacer(Modifier.height(12.dp))
                Featured(comics.first(), { onOpen(comics.first()) }, Modifier.padding(horizontal = 18.dp))
            }
        }
        item { ComicRail("تازه‌ترین‌ها", "به‌تازگی اضافه‌شده", comics.reversed(), onOpen) }
        item { ComicRail("ابرقهرمانی", "قهرمان‌ها و ماجراجویی", comics.filter { it.genre == "ابرقهرمانی" || it.genre == "علمی‌تخیلی" }, onOpen) }
        item { ComicRail("مانگا و مانهوا", "داستان‌های آسیایی", comics.filter { it.genre == "مانگا" || it.genre == "مانهوا" }, onOpen) }
        item { ComicRail("معمایی و فانتزی", "رازهایی برای کشف", comics.filter { it.genre == "معمایی" || it.genre == "علمی‌تخیلی" }, onOpen) }
        item {
            Text(
                "نسخه نمایشی • $savedCount عنوان ذخیره‌شده",
                color = Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun ComicRail(title: String, note: String, entries: List<Comic>, onOpen: (Comic) -> Unit) {
    Column {
        Column(Modifier.padding(horizontal = 18.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(note, color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            entries.forEach { comic ->
                Column(Modifier.width(124.dp).clickable { onOpen(comic) }) {
                    Cover(comic, Modifier.fillMaxWidth().height(166.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(comic.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text(comic.genre, color = comic.accent, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${comic.chapters} فصل", color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable private fun Heading(title: String, note: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(note, color = Cyan, fontSize = 11.sp)
    }
}

@Composable private fun Cover(comic: Comic, modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(comic.accent.copy(alpha = .6f), Color(0xFF11172B))), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(comic.symbol, color = Color.White, fontSize = 40.sp)
            Text(comic.english, color = Color.White.copy(alpha = .8f), fontSize = 8.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable private fun Featured(comic: Comic, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(comic, Modifier.width(102.dp).height(140.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(comic.english, color = comic.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(comic.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(comic.description, color = Muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(comic.genre + " • " + comic.chapters + " فصل", color = Cyan, fontSize = 11.sp)
        }
    }
}

@Composable private fun ComicRow(comic: Comic, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).background(Panel, RoundedCornerShape(18.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(comic, Modifier.width(76.dp).height(98.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(comic.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(comic.english, color = comic.accent, fontSize = 10.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(5.dp))
            Text(comic.description, color = Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Text(comic.genre + " • " + comic.chapters + " فصل", color = Cyan, fontSize = 10.sp)
        }
        Icon(Icons.Default.ChevronLeft, null, tint = Muted)
    }
}

@Composable private fun SearchScreen(query: String, onQuery: (String) -> Unit, genre: String, onGenre: (String) -> Unit, results: List<Comic>, onOpen: (Comic) -> Unit, modifier: Modifier = Modifier) {
    val genres = listOf("همه", "ابرقهرمانی", "مانگا", "مانهوا", "علمی‌تخیلی", "معمایی")
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("جست‌وجو و کشف", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 25.dp, bottom = 16.dp))
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text("نام داستان یا ژانر...", color = Muted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Cyan) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Line, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            genres.forEach { g -> FilterChip(selected = genre == g, onClick = { onGenre(g) }, label = { Text(g) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Purple, selectedLabelColor = Night, labelColor = Muted)) }
        }
        Spacer(Modifier.height(12.dp))
        if (results.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("چیزی پیدا نشد؛ عبارت دیگری را امتحان کن.", color = Muted) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) { items(results) { ComicRow(it) { onOpen(it) } } }
    }
}

@Composable private fun LibraryScreen(comicsSaved: List<Comic>, onOpen: (Comic) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("کتابخانه من", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
        Text("داستان‌هایی که ذخیره کرده‌ای", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 18.dp))
        if (comicsSaved.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Bookmarks, null, tint = Purple, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("کتابخانه‌ات هنوز خالی است", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("از صفحه جزئیات، داستان‌ها را ذخیره کن.", color = Muted, fontSize = 12.sp)
                }
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(comicsSaved) { ComicRow(it) { onOpen(it) } } }
    }
}

@Composable private fun DetailScreen(comic: Comic, isSaved: Boolean, onSave: () -> Unit, onRead: (Int) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, "بازگشت", tint = Color.White) }
            Text("جزئیات داستان", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(comic, Modifier.width(126.dp).height(175.dp))
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text(comic.english, color = comic.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(comic.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(comic.genre, color = Cyan, fontSize = 13.sp)
                    Text(comic.chapters.toString() + " فصل • نسخه نمایشی", color = Muted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("درباره داستان", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(comic.description, color = Muted, fontSize = 14.sp, lineHeight = 23.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onRead(1) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Night), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.MenuBook, null); Spacer(Modifier.width(8.dp)); Text("شروع مطالعه", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp), shape = RoundedCornerShape(15.dp)) { Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null, tint = Cyan); Spacer(Modifier.width(8.dp)); Text(if (isSaved) "حذف از کتابخانه" else "ذخیره در کتابخانه", color = Color.White) }
            Spacer(Modifier.height(20.dp))
            Text("فصل‌ها", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text("یک فصل را برای باز کردن پیش‌نمایش انتخاب کن.", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..comic.chapters).toList().chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { chapterNumber ->
                            OutlinedButton(
                                onClick = { onRead(chapterNumber) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Text("فصل $chapterNumber", color = Color.White, fontSize = 12.sp)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable private fun ReaderScreen(comic: Comic, chapter: Int, mode: String, onMode: (String) -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, "بازگشت", tint = Color.White) }
            Column { Text(comic.title, color = Color.White, fontWeight = FontWeight.Bold); Text("فصل $chapter از ${comic.chapters} • پیش‌نمایش", color = Muted, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(5.dp)) {
            listOf("صفحه‌ای", "پیوسته").forEach { item ->
                Box(Modifier.weight(1f).background(if (mode == item) PanelHigh else Color.Transparent, RoundedCornerShape(10.dp)).clickable { onMode(item) }.padding(11.dp), contentAlignment = Alignment.Center) { Text(item, color = if (mode == item) Cyan else Muted, fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(Modifier.height(15.dp))
        Box(Modifier.fillMaxWidth().weight(1f).background(Brush.verticalGradient(listOf(PanelHigh, Panel)), RoundedCornerShape(20.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(comic.symbol, color = comic.accent, fontSize = 72.sp)
                Spacer(Modifier.height(15.dp))
                Text(comic.english, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("پیش‌نمایش محیط مطالعه", color = Cyan, fontSize = 13.sp)
                Spacer(Modifier.height(22.dp))
                Text("فصل $chapter", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("حالت " + mode + " انتخاب شده است.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("این پیش‌نمایش است؛ صفحات واقعی کمیک هنوز اضافه نشده‌اند.", color = Muted, fontSize = 11.sp, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPrevious, enabled = chapter > 1, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("فصل قبلی") }
            Button(onClick = onNext, enabled = chapter < comic.chapters, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Night)) { Text("فصل بعدی") }
        }
        Spacer(Modifier.height(8.dp))
        Text("نسخه نمایشی • محتوای دارای مجوز باید اضافه شود", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable private fun AccountScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp)) {
        Text("حساب کاربری", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        Text("پروفایل کامینگا در حال آماده‌سازی است.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(76.dp).background(PanelHigh, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Cyan, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(18.dp))
        Text("ورود و ثبت‌نام هنوز فعال نیست", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        Text("در مرحله بعد معماری حساب کاربری را بررسی می‌کنیم. تأیید پیامکی به سرویس جداگانه و احتمالاً هزینه نیاز دارد.", color = Muted, fontSize = 13.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, null, tint = Purple)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("امنیت و حریم خصوصی", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("اطلاعات حساس در نسخه نمایشی ذخیره نمی‌شود.", color = Muted, fontSize = 11.sp)
            }
        }
    }
}
