package com.kaminga.reader

import android.os.Bundle
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Night = Color(0xFFF1F7FA) // روشنِ آبیِ خیلی ملایم
private val Panel = Color(0xFFFFFFFF)
private val PanelHigh = Color(0xFFDDF2EE)
private val Purple = Color(0xFF347DA8) // آبی اصلی
private val Cyan = Color(0xFF3D9B83) // سبز ملایم
private val Muted = Color(0xFF617782)
private val Line = Color(0xFFD5E4E9)
private val Ink = Color(0xFF203744)

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
        window.statusBarColor = android.graphics.Color.rgb(241, 247, 250)
        window.navigationBarColor = android.graphics.Color.rgb(241, 247, 250)
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
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
    BackHandler(enabled = reading != null || selected != null || tab != "خانه") {
        when {
            reading != null -> reading = null
            selected != null -> selected = null
            tab != "خانه" -> tab = "خانه"
        }
    }
    val preferences = remember { context.getSharedPreferences("kaminga_local", Context.MODE_PRIVATE) }
    val saved = remember {
        mutableStateListOf<String>().apply {
            addAll(preferences.getStringSet("saved_comics", emptySet())?.toList() ?: emptyList())
        }
    }
    val downloaded = remember {
        mutableStateListOf<String>().apply {
            addAll(preferences.getStringSet("downloaded_comics", emptySet())?.toList() ?: emptyList())
        }
    }
    val imagePageCounts = remember {
        mutableStateMapOf<String, Int>().apply {
            comics.forEach { comic ->
                val count = preferences.getInt("image_pages_${comic.title}", 0)
                if (count > 0) put(comic.title, count)
            }
        }
    }
    var importTargetTitle by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val title = importTargetTitle
        if (title != null && uris.isNotEmpty()) {
            val comic = comics.firstOrNull { it.title == title }
            if (comic != null) {
                val imported = copyComicImages(context, comic, uris)
                if (imported > 0) {
                    imagePageCounts[title] = imported
                    preferences.edit().putInt("image_pages_$title", imported).apply()
                    android.widget.Toast.makeText(context, "$imported صفحه تصویری اضافه شد.", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "تصویرها وارد نشدند؛ فایل‌های تصویری را انتخاب کن.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        importTargetTitle = null
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, secondary = Cyan, background = Night, surface = Panel, onPrimary = Ink, onBackground = Ink, onSurface = Ink)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = Night) {
                when {
                    reading != null -> ReaderScreen(
                        comic = reading!!,
                        chapter = readingChapter,
                        imagePaths = comicImagePaths(context, reading!!),
                        initialPage = preferences.getInt("page_${reading!!.title}_$readingChapter", 0),
                        mode = mode,
                        onMode = { mode = it },
                        onPageChange = { page ->
                            preferences.edit().putInt("page_${reading!!.title}_$readingChapter", page).apply()
                        },
                        onPrevious = {
                            readingChapter = (readingChapter - 1).coerceAtLeast(1)
                            preferences.edit()
                                .putInt("progress_${reading!!.title}", readingChapter)
                                .putLong("opened_${reading!!.title}", System.currentTimeMillis())
                                .apply()
                        },
                        onNext = {
                            readingChapter = (readingChapter + 1).coerceAtMost(reading!!.chapters)
                            preferences.edit()
                                .putInt("progress_${reading!!.title}", readingChapter)
                                .putLong("opened_${reading!!.title}", System.currentTimeMillis())
                                .apply()
                        },
                        onBack = { reading = null }
                    )
                    selected != null -> DetailScreen(
                        comic = selected!!,
                        isSaved = saved.contains(selected!!.title),
                        isDownloaded = downloaded.contains(selected!!.title),
                        importedImagePages = imagePageCounts[selected!!.title] ?: 0,
                        onImportImages = {
                            importTargetTitle = selected!!.title
                            imagePicker.launch(arrayOf("image/*"))
                        },
                        resumeChapter = preferences.getInt("progress_${selected!!.title}", 1).coerceIn(1, selected!!.chapters),
                        onSave = {
                            if (saved.contains(selected!!.title)) saved.remove(selected!!.title) else saved.add(selected!!.title)
                            preferences.edit().putStringSet("saved_comics", saved.toSet()).apply()
                        },
                        onDownload = {
                            val comic = selected!!
                            val shouldDownload = !downloaded.contains(comic.title)
                            if (setOfflinePreview(context, comic, shouldDownload)) {
                                if (shouldDownload) downloaded.add(comic.title) else downloaded.remove(comic.title)
                                preferences.edit().putStringSet("downloaded_comics", downloaded.toSet()).apply()
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "ذخیره‌سازی انجام نشد؛ دوباره تلاش کن.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onRead = { chapter ->
                            readingChapter = chapter
                            preferences.edit()
                                .putInt("progress_${selected!!.title}", chapter)
                                .putLong("opened_${selected!!.title}", System.currentTimeMillis())
                                .apply()
                            reading = selected
                        },
                        onBack = { selected = null }
                    )
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
                            "کتابخانه" -> LibraryScreen(
                                comics.filter { saved.contains(it.title) || downloaded.contains(it.title) },
                                { selected = it },
                                Modifier.padding(padding)
                            )
                            "حساب" -> AccountScreen(Modifier.padding(padding))
                            else -> HomeScreen(
                                savedCount = saved.size,
                                recentlyRead = comics
                                    .filter { preferences.getLong("opened_${it.title}", 0L) > 0L }
                                    .sortedByDescending { preferences.getLong("opened_${it.title}", 0L) },
                                onSearch = { tab = "جست‌وجو" },
                                onOpen = { selected = it },
                                modifier = Modifier.padding(padding)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    savedCount: Int,
    recentlyRead: List<Comic>,
    onSearch: () -> Unit,
    onOpen: (Comic) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    Text("KAMINGA", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("داستانی برای هر سلیقه", color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = onSearch, modifier = Modifier.background(Panel, CircleShape)) {
                    Icon(Icons.Default.Search, contentDescription = "جست‌وجو", tint = Ink)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Text("دنیای داستان‌ها", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("کمیک، مانگا و مانهوا را کشف کن.", color = Muted, fontSize = 13.sp)
            }
        }
        if (recentlyRead.isNotEmpty()) {
            item { ComicRail("ادامه مطالعه", "آخرین داستان‌هایی که باز کرده‌ای", recentlyRead, onOpen) }
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
            Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    Text(comic.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(note, color = Cyan, fontSize = 11.sp)
    }
}

@Composable private fun Cover(comic: Comic, modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(comic.accent.copy(alpha = .6f), Color(0xFF11172B))), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(comic.symbol, color = Color.White, fontSize = 40.sp)
            Text(comic.english, color = Color.White.copy(alpha = .9f), fontSize = 8.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable private fun Featured(comic: Comic, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(comic, Modifier.width(102.dp).height(140.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(comic.english, color = comic.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(comic.title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
            Text(comic.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
        Text("جست‌وجو و کشف", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 25.dp, bottom = 16.dp))
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text("نام داستان یا ژانر...", color = Muted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Cyan) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Line, focusedTextColor = Ink, unfocusedTextColor = Ink))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            genres.forEach { g -> FilterChip(selected = genre == g, onClick = { onGenre(g) }, label = { Text(g) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Purple, selectedLabelColor = Night, labelColor = Muted)) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${results.size} عنوان", color = Muted, fontSize = 11.sp)
            if (query.isNotBlank() || genre != "همه") {
                TextButton(onClick = { onQuery(""); onGenre("همه") }) { Text("پاک‌کردن فیلترها", color = Cyan, fontSize = 11.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (results.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("چیزی پیدا نشد؛ عبارت دیگری را امتحان کن.", color = Muted) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) { items(results) { ComicRow(it) { onOpen(it) } } }
    }
}

@Composable private fun LibraryScreen(comicsSaved: List<Comic>, onOpen: (Comic) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("کتابخانه من", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
        Text("داستان‌های ذخیره‌شده و پیش‌نمایش‌های آفلاین", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 18.dp))
        if (comicsSaved.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Bookmarks, null, tint = Purple, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("کتابخانه‌ات هنوز خالی است", color = Ink, fontWeight = FontWeight.Bold)
                    Text("از صفحه جزئیات، داستان‌ها را ذخیره کن.", color = Muted, fontSize = 12.sp)
                }
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(comicsSaved) { ComicRow(it) { onOpen(it) } } }
    }
}

@Composable private fun DetailScreen(
    comic: Comic,
    isSaved: Boolean,
    isDownloaded: Boolean,
    importedImagePages: Int,
    onImportImages: () -> Unit,
    resumeChapter: Int,
    onSave: () -> Unit,
    onDownload: () -> Unit,
    onRead: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, "بازگشت", tint = Ink) }
            Text("جزئیات داستان", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(comic, Modifier.width(126.dp).height(175.dp))
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text(comic.english, color = comic.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(comic.title, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(comic.genre, color = Cyan, fontSize = 13.sp)
                    Text(comic.chapters.toString() + " فصل • نسخه نمایشی", color = Muted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("درباره داستان", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(comic.description, color = Muted, fontSize = 14.sp, lineHeight = 23.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onRead(resumeChapter) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Ink), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Default.MenuBook, null)
                Spacer(Modifier.width(8.dp))
                Text(if (resumeChapter > 1) "ادامه از فصل $resumeChapter" else "شروع مطالعه", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null, tint = Cyan)
                Spacer(Modifier.width(8.dp))
                Text(if (isSaved) "حذف از کتابخانه" else "ذخیره در کتابخانه", color = Ink)
            }
            OutlinedButton(
                onClick = onImportImages,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Default.Collections, null, tint = Cyan)
                Spacer(Modifier.width(8.dp))
                Text(if (importedImagePages > 0) "مدیریت صفحات تصویری ($importedImagePages)" else "افزودن صفحات تصویری از گوشی", color = Ink)
            }
            Text(
                "می‌توانی تصاویر کمیک مجاز را از حافظه گوشی انتخاب کنی؛ برنامه آن‌ها را داخل خودش ذخیره می‌کند.",
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 5.dp)
            )
            OutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(if (isDownloaded) Icons.Default.DeleteOutline else Icons.Default.Download, null, tint = Cyan)
                Spacer(Modifier.width(8.dp))
                Text(if (isDownloaded) "حذف پیش‌نمایش آفلاین" else "ذخیره پیش‌نمایش آفلاین", color = Ink)
            }
            Text(
                if (isDownloaded) "پیش‌نمایش متنی این عنوان روی گوشی ذخیره شده است." else "دانلود فعلاً فقط پیش‌نمایش متنی نمایشی را ذخیره می‌کند؛ فایل کمیک اصلی هنوز موجود نیست.",
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 7.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text("فصل‌ها", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                                Text("فصل $chapterNumber", color = Ink, fontSize = 12.sp)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun demoChapterPages(comic: Comic, chapter: Int): List<String> {
    val opening = when (comic.genre) {
        "علمی‌تخیلی" -> "در لبه‌ی ایستگاه، چراغ‌های هشدار یکی پس از دیگری روشن شدند. پیام ناشناس فقط یک جمله داشت: «به آسمان اعتماد نکن.»"
        "ابرقهرمانی" -> "شهر آرام به نظر می‌رسید، اما صدای آژیر از خیابان دوردست می‌آمد. او می‌دانست این بار نمی‌تواند پشت سایه‌ها پنهان بماند."
        "معمایی" -> "روی میز، یادداشتی بی‌امضا کنار ساعت متوقف‌شده قرار داشت. سرنخ کوچک بود، اما تمام جزئیات اتاق را تغییر می‌داد."
        "مانگا" -> "باد از میان درختان گذشت و برگ‌ها را روی مسیر سنگی پخش کرد. سفر تازه آغاز شده بود و پاسخ‌ها هنوز دور بودند."
        else -> "نور ماه روی پنجره افتاده بود و دو جهان برای لحظه‌ای به هم نزدیک شدند. انتخاب بعدی می‌توانست همه‌چیز را عوض کند."
    }
    return listOf(
        "فصل $chapter\n\n$opening",
        "قدم بعدی آسان نبود. هر نشانه پرسش تازه‌ای می‌ساخت و قهرمان داستان باید تصمیم می‌گرفت به کدام صدا اعتماد کند.\n\n${comic.description}",
        "پایان بخش اول\n\nراز هنوز کامل آشکار نشده است. ادامه‌ی مسیر در فصل بعدی منتظر توست.\n\nاین متن نمونه برای آزمایش محیط مطالعه است؛ صفحات اصلی و دارای مجوز هنوز اضافه نشده‌اند."
    )
}

@Composable private fun ReaderScreen(
    comic: Comic,
    chapter: Int,
    imagePaths: List<String>,
    initialPage: Int,
    mode: String,
    onMode: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val pages = remember(comic.title, chapter) { demoChapterPages(comic, chapter) }
    val hasImages = imagePaths.isNotEmpty()
    val pageCount = if (hasImages) imagePaths.size else pages.size
    var page by rememberSaveable(comic.title, chapter, imagePaths.size) {
        mutableIntStateOf(initialPage.coerceIn(0, pageCount - 1))
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, "بازگشت", tint = Ink) }
            Column(Modifier.weight(1f)) {
                Text(comic.title, color = Ink, fontWeight = FontWeight.Bold)
                Text("فصل $chapter از ${comic.chapters}", color = Muted, fontSize = 11.sp)
            }
            Text("${page + 1}/$pageCount", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(5.dp)) {
            listOf("صفحه‌ای", "پیوسته").forEach { item ->
                Box(Modifier.weight(1f).background(if (mode == item) PanelHigh else Color.Transparent, RoundedCornerShape(10.dp)).clickable { onMode(item) }.padding(10.dp), contentAlignment = Alignment.Center) {
                    Text(item, color = if (mode == item) Cyan else Muted, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (mode == "صفحه‌ای") {
            Column(
                Modifier.fillMaxWidth().weight(1f).background(Panel, RoundedCornerShape(20.dp)).padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (hasImages) {
                    LocalComicImage(imagePaths[page], Modifier.fillMaxWidth().weight(1f))
                } else {
                    Cover(comic, Modifier.fillMaxWidth().height(130.dp))
                    Spacer(Modifier.height(22.dp))
                    Text("صفحه ${page + 1}", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(pages[page], color = Ink, fontSize = 16.sp, lineHeight = 29.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (page > 0) {
                            page--
                            onPageChange(page)
                        } else if (!hasImages) onPrevious()
                    },
                    enabled = page > 0 || (!hasImages && chapter > 1),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (page > 0 || hasImages) "صفحه قبل" else "فصل قبلی") }
                Button(
                    onClick = {
                        if (page < pageCount - 1) {
                            page++
                            onPageChange(page)
                        } else if (!hasImages && chapter < comic.chapters) onNext()
                    },
                    enabled = page < pageCount - 1 || (!hasImages && chapter < comic.chapters),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Ink)
                ) { Text(if (page < pageCount - 1 || hasImages) "صفحه بعد" else "فصل بعدی") }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(pageCount) { index ->
                    if (hasImages) {
                        LocalComicImage(
                            imagePaths[index],
                            Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(6.dp)
                        )
                    } else {
                        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(18.dp)) {
                            Text("صفحه ${index + 1}", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            if (index == 0) Cover(comic, Modifier.fillMaxWidth().height(120.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(pages[index], color = Ink, fontSize = 15.sp, lineHeight = 27.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = !hasImages && chapter > 1,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("فصل قبلی") }
                Button(
                    onClick = onNext,
                    enabled = !hasImages && chapter < comic.chapters,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Ink)
                ) { Text("فصل بعدی") }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasImages) "صفحات تصویری واردشده از گوشی • برای استفاده فقط محتوای مجاز وارد کن"
            else "محتوای نمونه برای آزمایش مطالعه • می‌توانی صفحات تصویری را از جزئیات اثر وارد کنی",
            color = Muted,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}




private fun comicImageDirectory(context: Context, comic: Comic) =
    java.io.File(context.filesDir, "comic_pages_${comic.title.hashCode()}")

private fun comicImagePaths(context: Context, comic: Comic): List<String> =
    comicImageDirectory(context, comic).listFiles()
        ?.filter { it.isFile }
        ?.sortedBy { it.name }
        ?.map { it.absolutePath }
        ?: emptyList()

private fun copyComicImages(context: Context, comic: Comic, uris: List<android.net.Uri>): Int {
    val directory = comicImageDirectory(context, comic)
    return try {
        directory.deleteRecursively()
        directory.mkdirs()
        var count = 0
        uris.forEachIndexed { index, uri ->
            val target = java.io.File(directory, "page_%04d.img".format(index + 1))
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (copied) count++
            else target.delete()
        }
        if (count == 0) directory.deleteRecursively()
        count
    } catch (_: Exception) {
        directory.deleteRecursively()
        0
    }
}

@Composable
private fun LocalComicImage(path: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, options)
                var sample = 1
                while (options.outWidth / sample > 1800 || options.outHeight / sample > 2600) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            } catch (_: Exception) { null }
        }
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "صفحه کمیک",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier.height(180.dp), contentAlignment = Alignment.Center) {
            Text("این تصویر قابل نمایش نیست.", color = Muted, fontSize = 12.sp)
        }
    }
}

private fun setOfflinePreview(context: Context, comic: Comic, shouldDownload: Boolean): Boolean {
    val prefix = "kaminga_${comic.title.hashCode()}_"
    return try {
        if (shouldDownload) {
            for (chapter in 1..comic.chapters) {
                val content = demoChapterPages(comic, chapter).joinToString("\n\n──────────\n\n")
                context.openFileOutput("${prefix}${chapter}.txt", Context.MODE_PRIVATE).use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                }
            }
        } else {
            context.filesDir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
        }
        true
    } catch (_: Exception) {
        false
    }
}

@Composable private fun AccountScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp)) {
        Text("حساب کاربری", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        Text("پروفایل کامینگا در حال آماده‌سازی است.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(76.dp).background(PanelHigh, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, tint = Cyan, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("ورود و ثبت‌نام هنوز فعال نیست", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        Text("در مرحله بعد معماری حساب کاربری را بررسی می‌کنیم. تأیید پیامکی به سرویس جداگانه و احتمالاً هزینه نیاز دارد.", color = Muted, fontSize = 13.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, null, tint = Purple)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("امنیت و حریم خصوصی", color = Ink, fontWeight = FontWeight.SemiBold)
                Text("اطلاعات حساس در نسخه نمایشی ذخیره نمی‌شود.", color = Muted, fontSize = 11.sp)
            }
        }
    }
}
