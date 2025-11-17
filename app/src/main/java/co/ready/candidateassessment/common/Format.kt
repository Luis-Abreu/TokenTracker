package co.ready.candidateassessment.common

import android.annotation.SuppressLint
import android.util.Log
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTokenBalance(rawBalance: String, decimals: Int): String = try {
    // parse the raw balance as a BigInteger
    val balanceBigInt = BigInteger(rawBalance)

    // convert to BigDecimal and divide by 10^decimals
    val divisor = BigDecimal.TEN.pow(decimals)
    val actualBalance =
        BigDecimal(balanceBigInt).divide(divisor, decimals, RoundingMode.DOWN)

    // determine appropriate scale for display
    val displayScale = when {
        actualBalance >= BigDecimal("1000") -> 2 // for large balances show 2 decimals
        actualBalance >= BigDecimal("1") -> 4 // for medium balances show 4 decimals
        actualBalance > BigDecimal.ZERO -> 6 // for small balances show 6 decimals
        else -> 0 // for zero balance
    }

    // round to display scale
    val roundedBalance = actualBalance.setScale(displayScale, RoundingMode.DOWN)

    // strip trailing zeros for cleaner display
    val strippedBalance = roundedBalance.stripTrailingZeros()

    // format with thousand separators
    val formatter = DecimalFormat("#,##0.##########")
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = displayScale

    formatter.format(strippedBalance)
} catch (e: Exception) {
    Log.e("TokenBalanceFormatter", "Error formatting balance: $rawBalance", e)
    rawBalance
}

fun formatTimestamp(timestamp: Long): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return simpleDateFormat.format(Date(timestamp))
}

@SuppressLint("DefaultLocale")
fun formatLargeNumber(number: Double): String = when {
    number >= 1_000_000_000 -> "$${String.format("%.2f", number / 1_000_000_000)}B"
    number >= 1_000_000 -> "$${String.format("%.2f", number / 1_000_000)}M"
    number >= 1_000 -> "$${String.format("%.2f", number / 1_000)}K"
    else -> "$${String.format("%.2f", number)}"
}
