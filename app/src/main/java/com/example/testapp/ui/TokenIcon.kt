package ui

import androidx.compose.runtime.Composable
import ltd.mbor.minimak.Balance

@Composable
fun TokenIcon(url: String) {
  //TODO
}

@Composable
fun TokenIcon(tokenId: String, balances: Map<String, Balance>) {
  TokenIcon(balances[tokenId]?.tokenUrl?.takeIf { it.isNotBlank() }
    ?: if (tokenId == "0x00") "minima.svg" else "coins.svg")
}