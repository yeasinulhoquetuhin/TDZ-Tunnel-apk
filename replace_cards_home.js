const fs = require('fs');

const file = 'app/src/main/java/com/example/ui/HomeScreen.kt';

let text = fs.readFileSync(file, 'utf8');

// add import
if (!text.includes('com.example.ui.theme.glassmorphicBackground')) {
  text = text.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\nimport com.example.ui.theme.glassmorphicBackground');
}

text = text.replace(/colors = CardDefaults\.cardColors\(containerColor = MaterialTheme\.colorScheme\.surfaceVariant\),\s*shape = RoundedCornerShape\(24\.dp\)/g, 
  'modifier = Modifier.fillMaxWidth().glassmorphicBackground(MaterialTheme.colorScheme.primary, 0.08f),\n            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),\n            shape = RoundedCornerShape(24.dp)');

fs.writeFileSync(file, text);
