package com.example.yds_kelime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.yds_kelime.ui.theme.Yds_kelimeTheme

val dmSansFamily = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_bold, FontWeight.Bold)
)

class MainActivity : ComponentActivity() {
    private lateinit var wordManager: WordManager
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordManager = WordManager(this)
        soundManager = SoundManager(this)

        setContent {
            Yds_kelimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFCF8E8)
                ) {
                    WordLearningScreen(wordManager, soundManager)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

@Composable
fun WordLearningScreen(wordManager: WordManager, soundManager: SoundManager) {
    var currentWord by remember { mutableStateOf(wordManager.getCurrentWord()) }
    var showSettings by remember { mutableStateOf(false) }
    var isTransitioning by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        currentWord = wordManager.getCurrentWord()
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    fun nextWord(markFunction: () -> Unit, sound: () -> Unit) {
        if (isTransitioning) return
        isTransitioning = true
        
        sound()
        coroutineScope.launch {
            delay(100)
            markFunction()
            currentWord = wordManager.getCurrentWord()
            delay(100)
            isTransitioning = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (currentWord != null) {
                key(currentWord!!.english) {
                    var localFlipped by remember { mutableStateOf(false) }
                    
                    FlipCard(
                        word = currentWord!!,
                        isFlipped = localFlipped,
                        onFlip = { 
                            localFlipped = !localFlipped
                            soundManager.playFlipSound()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                nextWord(
                                    { wordManager.markAsWrong() },
                                    { soundManager.playWrongSound() }
                                )
                            },
                            enabled = localFlipped && !isTransitioning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDF7861),
                                disabledContainerColor = Color(0xFFBDBDBD)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(70.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Yanlış",
                                tint = if (localFlipped) Color.White else Color(0xFF757575),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Button(
                            onClick = {
                                nextWord(
                                    { wordManager.markAsCorrect() },
                                    { soundManager.playCorrectSound() }
                                )
                            },
                            enabled = localFlipped && !isTransitioning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD4E2D4),
                                disabledContainerColor = Color(0xFFBDBDBD)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(70.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Doğru",
                                tint = if (localFlipped) Color(0xFF2E7D32) else Color(0xFF757575),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Tebrikler! Tüm kelimeleri tamamladınız!",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        FloatingActionButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
        }
    }

    if (showSettings) {
        SettingsDialog(
            wordManager = wordManager,
            soundManager = soundManager,
            onDismiss = { showSettings = false },
            onWordAdded = {
                currentWord = wordManager.getCurrentWord()
            }
        )
    }
}

@Composable
fun FlipCard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFlipped) Color(0xFFD4E2D4) else Color(0xFFECB390)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (rotation <= 90f) {
                    Text(
                        word.english,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                        fontFamily = dmSansFamily
                    )
                } else {
                    Text(
                        word.turkish,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp,
                        fontFamily = dmSansFamily,
                        modifier = Modifier.graphicsLayer { rotationY = 180f }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    wordManager: WordManager,
    soundManager: SoundManager,
    onDismiss: () -> Unit,
    onWordAdded: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var englishText by remember { mutableStateOf("") }
    var turkishText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(1) }
    var soundEnabled by remember { mutableStateOf(soundManager.soundEnabled) }
    
    val (correct, wrong, remaining) = wordManager.getStats()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFFCF8E8)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    ) {
                        Text(
                            "📊",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 20.sp
                        )
                    }
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    ) {
                        Text(
                            "➕",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 20.sp
                        )
                    }
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    ) {
                        Text(
                            "📝",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 20.sp
                        )
                    }
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    ) {
                        Text(
                            "⚙️",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 20.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (selectedTab == 0 || selectedTab == 3) Arrangement.Center else Arrangement.Top
                ) {
                    when (selectedTab) {
                        0 -> {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Öğrenme İlerlemen",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            StatRow("✅ Doğru Bilinen", correct.toString(), Color(0xFF66BB6A))
                            StatRow("❌ Yanlış Bilinen", wrong.toString(), Color(0xFFEF5350))
                            StatRow("📚 Kalan Kelime", remaining.toString(), Color(0xFF42A5F5))
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Kapat")
                            }
                        }
                        }
                        1 -> {
                        Text(
                            if (currentStep == 1) "Yeni Kelime Ekle" else "Türkçe Anlamı",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (currentStep == 1) {
                            OutlinedTextField(
                                value = englishText,
                                onValueChange = { 
                                    englishText = it
                                    errorMessage = ""
                                },
                                label = { Text("İngilizce Kelime") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            Text(
                                englishText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            OutlinedTextField(
                                value = turkishText,
                                onValueChange = { 
                                    turkishText = it
                                    errorMessage = ""
                                },
                                label = { Text("Türkçe Anlam") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        if (errorMessage.isNotEmpty()) {
                            Text(
                                errorMessage,
                                color = if (errorMessage == "Kelime eklendi!") Color(0xFF66BB6A) else Color.Red,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentStep == 2) {
                                OutlinedButton(
                                    onClick = { currentStep = 1 },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Geri")
                                }
                            }
                            
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("İptal")
                            }

                            Button(
                                onClick = {
                                    when (currentStep) {
                                        1 -> {
                                            if (englishText.isBlank()) {
                                                errorMessage = "Kelime boş olamaz"
                                            } else {
                                                currentStep = 2
                                            }
                                        }
                                        2 -> {
                                            if (turkishText.isBlank()) {
                                                errorMessage = "Anlam boş olamaz"
                                            } else {
                                                val success = wordManager.addCustomWord(englishText, turkishText)
                                                if (success) {
                                                    onWordAdded()
                                                    englishText = ""
                                                    turkishText = ""
                                                    currentStep = 1
                                                    errorMessage = "Kelime eklendi!"
                                                } else {
                                                    errorMessage = "Bu kelime zaten mevcut!"
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (currentStep == 1) "İleri" else "Ekle")
                            }
                        }
                        }
                        2 -> {
                            WordListScreen(wordManager, onDismiss)
                        }
                        3 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Uygulama Ayarları",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text("🔊", fontSize = 28.sp)
                                            Column {
                                                Text(
                                                    "Ses Efektleri",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Kartlar ve butonlar",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = soundEnabled,
                                            onCheckedChange = { 
                                                soundEnabled = it
                                                soundManager.soundEnabled = it
                                            }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Kapat")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(wordManager: WordManager, onDismiss: () -> Unit) {
    val wordList = remember { wordManager.getAllWordsList() }
    var editingWord by remember { mutableStateOf<Word?>(null) }
    var deleteConfirmWord by remember { mutableStateOf<Word?>(null) }
    
    if (editingWord != null) {
        EditWordDialog(
            word = editingWord!!,
            onDismiss = { editingWord = null },
            onSave = { newEnglish, newTurkish ->
                val success = wordManager.updateWord(editingWord!!, newEnglish, newTurkish)
                if (success) {
                    editingWord = null
                }
                success
            }
        )
    }
    
    if (deleteConfirmWord != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmWord = null },
            title = { Text("Kelimeyi Sil?") },
            text = { Text("'${deleteConfirmWord!!.english}' kelimesini silmek istediğinizden emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        wordManager.deleteWord(deleteConfirmWord!!)
                        deleteConfirmWord = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350)
                    )
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmWord = null }) {
                    Text("İptal")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "Kelime Listesi (${wordList.size})",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(wordList) { word ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                word.english,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                word.turkish,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { editingWord = word }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Düzenle",
                                    tint = Color(0xFF42A5F5)
                                )
                            }
                            IconButton(onClick = { deleteConfirmWord = word }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Sil",
                                    tint = Color(0xFFEF5350)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kapat")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWordDialog(
    word: Word,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Boolean
) {
    var englishText by remember { mutableStateOf(word.english) }
    var turkishText by remember { mutableStateOf(word.turkish) }
    var errorMessage by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Kelimeyi Düzenle",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = englishText,
                    onValueChange = { 
                        englishText = it
                        errorMessage = ""
                    },
                    label = { Text("İngilizce") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = turkishText,
                    onValueChange = { 
                        turkishText = it
                        errorMessage = ""
                    },
                    label = { Text("Türkçe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (errorMessage.isNotEmpty()) {
                    Text(
                        errorMessage,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İptal")
                    }
                    
                    Button(
                        onClick = {
                            if (englishText.isBlank() || turkishText.isBlank()) {
                                errorMessage = "Alanlar boş olamaz"
                            } else {
                                val success = onSave(englishText, turkishText)
                                if (!success) {
                                    errorMessage = "Bu kelime zaten mevcut!"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}
