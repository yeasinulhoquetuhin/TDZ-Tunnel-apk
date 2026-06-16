const fs = require('fs');
const file = 'app/src/main/java/com/example/ui/SettingsScreen.kt';
let text = fs.readFileSync(file, 'utf8');

text = text.replace(/colors = CardDefaults\.cardColors\(containerColor = MaterialTheme\.colorScheme\.surface\),\s*shape = RoundedCornerShape\(12\.dp\)/g, 
  'modifier = Modifier.fillMaxWidth().glassmorphicBackground(MaterialTheme.colorScheme.primary),\n            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),\n            shape = RoundedCornerShape(12.dp)');

text = text.replace(/colors = CardDefaults\.cardColors\(containerColor = MaterialTheme\.colorScheme\.surfaceVariant\.copy\((.*?)\)\),\s*shape = RoundedCornerShape\(12\.dp\)/g, 
  'modifier = Modifier.fillMaxWidth().glassmorphicBackground(MaterialTheme.colorScheme.primary, 0.08f),\n            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),\n            shape = RoundedCornerShape(12.dp)');

fs.writeFileSync(file, text);
