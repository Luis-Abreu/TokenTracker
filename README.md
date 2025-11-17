# A production-ready Android application that displays Ethereum token information with wallet balances, built following Clean Architecture principles and modern Android development practices.

## 📱 Features

- **Top Ethereum Tokens**: Displays the top 50 tokens from Ethplorer API
- **Wallet Balances**: Shows token balances for a specific Ethereum wallet address
- **Offline-First**: Cache-first strategy using Room database for seamless offline experience
- **Real-time Updates**: Live balance updates for each token
- **Material 3 Design**: Modern UI built entirely with Jetpack Compose
- **Error Handling**: Comprehensive error states with retry functionality

## 🛠️ Technology Stack

| Category | Technology |
|----------|-----------|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | Clean Architecture + MVVM |
| **Dependency Injection** | Hilt |
| **Networking** | Retrofit + OkHttp + Moshi |
| **Database** | Room |
| **Async/Concurrency** | Coroutines + Flow |
| **Image Loading** | Coil |
| **Build System** | Gradle with Version Catalog |

## 🏗️ Architecture Overview
```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (TokensViewModel, TokensScreen)        │
│         depends on ↓                    │
└─────────────────────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│           Domain Layer                  │
│    (TokenService interface, Token)      │
│         implemented by ↓                │
└─────────────────────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│            Data Layer                   │
│  (TokenRepository, API, Database)       │
└─────────────────────────────────────────┘
```

### **Presentation Layer** (`presentation/`)
- **ViewModels**: State management using `StateFlow`, lifecycle-aware data handling
- **UI Components**: Jetpack Compose screens with Material 3 design system
- **View States**: Transformed domain models optimized for UI consumption
- **Navigation**: Compose Navigation for screen transitions

**Key Files:**
- `TokensViewModel.kt` - Orchestrates data flow and UI state
- `TokensScreen.kt` - Main token list UI with error/loading states
- `WelcomeScreen.kt` - Entry screen with wallet information

### **Domain Layer** (`domain/`)
- **Interfaces**: `TokenService` defines the contract for token operations
- **Models**: Pure Kotlin data classes (`Token`, `TokenPrice`) with no Android dependencies
- **Business Rules**: Domain entities represent core business concepts

**Key Principles:**
- No dependencies on Android framework or external libraries
- Defines abstractions that data layer implements
- Easy to test and reuse across different platforms

### **Data Layer** (`data/`)
- **Repository**: `TokenRepository` implements `TokenService`, coordinates between network and cache
- **API Clients**: Retrofit interfaces for Etherscan and Ethplorer APIs
- **Database**: Room DAOs and entities for local persistence
- **Dependency Injection**: Hilt modules for providing dependencies

**Key Features:**
- **Cache-First Strategy**: Emits cached data immediately, then fetches fresh data
- **Offline Support**: Room database caches tokens and balances
- **Rate Limiting**: Custom interceptor for Etherscan API compliance
- **Error Handling**: Centralized `safeApiCall()` wrapper with `Outcome<T>` sealed class

## 🔑 Key Technical Decisions

### **Dependency Inversion**
The ViewModel depends on `TokenService` (interface) from the domain layer, not the concrete `TokenRepository`. This is enforced through:
- `TokenRepository` marked as `internal` (not accessible outside data layer)
- Hilt's `@Binds` annotation provides the implementation at runtime
- Enables easy mocking for tests and swapping implementations

### **Reactive Data Flow**
```kotlin
Repository → Flow<Outcome<T>> → ViewModel → StateFlow<State> → Composable UI
```
- `Flow` for asynchronous streams from repository
- `Outcome<T>` sealed class for type-safe state handling (Success/Error/Loading)
- `StateFlow` for UI state that survives configuration changes

### **Offline-First Pattern**
```kotlin
1. Emit cached data from Room (if available)
2. Fetch fresh data from network
3. Update cache and emit new data
```
This provides instant UI updates while ensuring data freshness.

### **Error Handling**
Centralized error handling using the `Outcome<T>` sealed class:
- HTTP errors with status codes
- Network connectivity issues
- JSON parsing errors
- Unexpected exceptions

All errors are logged and presented with user-friendly messages.

## 📂 Project Structure

```
app/src/main/java/co/ready/candidateassessment/
├── app/
│   ├── App.kt                    # Application class with Hilt setup
│   ├── MainActivity.kt           # Main activity with navigation
│   └── Constants.kt              # API keys and configuration
│
├── presentation/
│   ├── feature/
│   │   ├── token/
│   │   │   ├── TokensScreen.kt      # Token list UI
│   │   │   └── TokensViewModel.kt   # Token state management
│   │   └── welcome/
│   │       └── WelcomeScreen.kt     # Welcome screen UI
│   └── design/
│       ├── Theme.kt              # Material 3 theme
│       └── Color.kt              # Color palette
│
├── domain/
│   └── TokenService.kt           # Repository interface + domain models
│
├── data/
│   ├── TokenRepository.kt        # Repository implementation
│   ├── api/
│   │   ├── EtherscanApi.kt       # Etherscan API client
│   │   ├── EthplorerApi.kt       # Ethplorer API client
│   │   ├── RateLimitInterceptor.kt
│   │   └── PriceInfoAdapter.kt   # Custom Moshi adapter
│   ├── cache/
│   │   ├── TokenDatabase.kt      # Room database
│   │   ├── TokenDao.kt           # Token queries
│   │   ├── TokenBalanceDao.kt    # Balance queries
│   │   ├── TokenEntity.kt        # Token table entity
│   │   └── TokenBalanceEntity.kt # Balance table entity
│   └── di/
│       ├── NetworkModule.kt      # Network dependencies
│       ├── DatabaseModule.kt     # Database dependencies
│       ├── RepositoryModule.kt   # Repository binding
│       └── ImageModule.kt        # Image loading configuration
│
└── common/
    ├── Outcome.kt                # Sealed class for state handling
    └── Format.kt                 # Formatting utilities
```

## 🎯 Design Highlights

### **Visibility Modifiers for Encapsulation**
- All data layer classes (`TokenRepository`, DAOs, API clients) are marked `internal`
- Only domain interfaces and models are public
- Prevents bypassing abstractions even without multi-module setup

### **Type-Safe Error Handling**
```kotlin
sealed class Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>()
    data class Error(...) : Outcome<Nothing>()
    object Loading : Outcome<Nothing>()
}
```
Extension functions (`mapSuccess`, `onError`, etc.) enable functional-style transformations.

### **Performance Optimizations**
- Individual coroutines for each token balance fetch (parallel loading)
- Image caching via Coil
- Rate limiting interceptor for API compliance
- Database indices for fast queries
