package com.example.foodieclub.ui.common // O tu paquete preferido

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.foodieclub.R // Asegúrate que R esté accesible

@Composable
fun UserProfileHeader(
    photoUrl: String?,
    displayName: String?,
    memberSince: String?, // Se asume String formateado (ej. "2024-05-08")
    recipeCount: Int?,
    likeCount: Int? = null, // Opcional para MyProfile
    savedCount: Int? = null // Opcional para MyProfile
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(photoUrl).crossfade(true)
                .placeholder(R.drawable.ic_launcher_background) // Placeholder avatar
                .error(R.drawable.ic_broken_image_background) // Error avatar
                .fallback(R.drawable.ic_launcher_background)
                .build(),
            contentDescription = "Foto de perfil de $displayName",
            modifier = Modifier.size(100.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = displayName ?: "Usuario FoodieClub",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        memberSince?.let { // Mostrar solo si la fecha existe
            Text(
                text = "Miembro desde: ${it.split('T')[0]}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Fila para contadores
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            recipeCount?.let { Text("$it Recetas", style = MaterialTheme.typography.labelLarge) }
            likeCount?.let { Text("$it Me gusta", style = MaterialTheme.typography.labelLarge) }
            savedCount?.let { Text("$it Guardados", style = MaterialTheme.typography.labelLarge) }
        }
    }
}