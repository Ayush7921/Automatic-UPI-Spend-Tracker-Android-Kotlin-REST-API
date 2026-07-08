package com.example.upipaymenttracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upipaymenttracker.TransactionModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun SpendingMap(
    transactions: List<TransactionModel>,
    modifier: Modifier = Modifier
) {
    val mapTransactions = transactions.filter { it.latitude != null && it.longitude != null }
    
    if (mapTransactions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(16.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(text = "No location data available.")
        }
        return
    }

    val cameraPositionState = rememberCameraPositionState {
        val firstLoc = mapTransactions.first()
        position = CameraPosition.fromLatLngZoom(
            LatLng(firstLoc.latitude!!, firstLoc.longitude!!), 
            10f
        )
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Spending Map",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                mapTransactions.forEach { trans ->
                    Marker(
                        state = rememberMarkerState(
                            key = trans.id.toString(),
                            position = LatLng(trans.latitude!!, trans.longitude!!)
                        ),
                        title = "₹${trans.amount}",
                        snippet = trans.note.ifEmpty { trans.category }
                    )
                }
            }
        }
    }
}