import os
import re

file_path = "app/src/main/java/com/example/ui/SettingsScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

pattern = r'colors\s*=\s*CardDefaults\.cardColors\(containerColor\s*=\s*MaterialTheme\.colorScheme\.surface\),\s*\n\s*shape\s*=\s*RoundedCornerShape\(12\.dp\)'
replacement = r"""modifier = Modifier.fillMaxWidth().glassmorphicBackground(MaterialTheme.colorScheme.primary),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            shape = RoundedCornerShape(12.dp)"""

text = re.sub(pattern, replacement, text)

with open(file_path, "w") as f:
    f.write(text)
