package uqu.drawbridge.posdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uqu.drawbridge.posdemo.ui.components.BarcodeScannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        Scaffold(
            topBar = {
                OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("POS Scanner Demo", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF102A3D),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFF03111F)
        ) { paddingValues ->
            var scannedValue by remember { mutableStateOf<String?>(null) }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (scannedValue != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162F43))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Last Scanned:", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            Text(scannedValue!!, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { scannedValue = null }, modifier = Modifier.fillMaxWidth()) {
                                Text("Scan Again")
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black, shape = MaterialTheme.shapes.medium)
                    ) {
                        BarcodeScannerView(
                            onBarcodeScanned = { barcode ->
                                scannedValue = barcode
                            },
                            onClose = {}
                        )
                    }
                }
            }
        }
    }
}
