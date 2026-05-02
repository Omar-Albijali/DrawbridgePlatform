package uqu.drawbridge.platform.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppTextField
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.model.SignupFormState

@Composable
internal fun WelcomeAuthScreen(
    onGoToLogin: () -> Unit,
    onGoToSignup: () -> Unit,
) {
    ScreenSection(
        title = "Welcome to Drawbridge",
        subtitle = "Mobile-first operations for wholesalers and retailers.",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrimaryButton(text = "Sign In", onClick = onGoToLogin)
            SecondaryButton(text = "Create Account", onClick = onGoToSignup)
        }
    }
}

@Composable
internal fun LoginAuthScreen(
    onSubmit: (email: String, password: String, rememberMe: Boolean) -> Unit,
    onGoToSignup: () -> Unit,
    onBack: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

    ScreenSection(
        title = "Sign In",
        subtitle = "Use your business account to continue.",
    ) {
        AppCard {
            AppTextField(
                value = email,
                onValueChange = { email = it },
                label = "Business email",
                keyboardType = KeyboardType.Email,
            )
            AppTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = rememberMe,
                    onClick = { rememberMe = true },
                    label = { Text("Remember") },
                )
                FilterChip(
                    selected = !rememberMe,
                    onClick = { rememberMe = false },
                    label = { Text("Session") },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                PrimaryButton(text = "Continue", onClick = { onSubmit(email.trim(), password, rememberMe) })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onBack) {
                        Text("Back", style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(onClick = onGoToSignup) {
                        Text("Register", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SignupAuthScreen(
    onSubmit: (SignupFormState) -> Unit,
    onGoToLogin: () -> Unit,
    onBack: () -> Unit,
) {
    var form by remember { mutableStateOf(SignupFormState()) }

    ScreenSection(
        title = "Create Account",
        subtitle = "Register your business profile to access platform tools.",
    ) {
        AppCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = form.role == UserRole.RETAILER,
                    onClick = { form = form.copy(role = UserRole.RETAILER) },
                    label = { Text("Retailer") },
                )
                FilterChip(
                    selected = form.role == UserRole.WHOLESALER,
                    onClick = { form = form.copy(role = UserRole.WHOLESALER) },
                    label = { Text("Wholesaler") },
                )
            }
        }

        SignupGroup("Business") {
            AppTextField(value = form.companyName, onValueChange = { form = form.copy(companyName = it) }, label = "Company")
            AppTextField(value = form.businessEmail, onValueChange = { form = form.copy(businessEmail = it) }, label = "Business email", keyboardType = KeyboardType.Email)
            AppTextField(value = form.phoneNumber, onValueChange = { form = form.copy(phoneNumber = it) }, label = "Phone", keyboardType = KeyboardType.Phone)
            AppTextField(value = form.commercialRegistration, onValueChange = { form = form.copy(commercialRegistration = it) }, label = "Commercial registration")
        }

        SignupGroup("Representative") {
            AppTextField(value = form.repName, onValueChange = { form = form.copy(repName = it) }, label = "Name")
            AppTextField(value = form.repJobTitle, onValueChange = { form = form.copy(repJobTitle = it) }, label = "Job title")
            AppTextField(value = form.repPhone, onValueChange = { form = form.copy(repPhone = it) }, label = "Phone", keyboardType = KeyboardType.Phone)
            AppTextField(value = form.repEmail, onValueChange = { form = form.copy(repEmail = it) }, label = "Email", keyboardType = KeyboardType.Email)
        }

        SignupGroup("Address") {
            AppTextField(value = form.street, onValueChange = { form = form.copy(street = it) }, label = "Street")
            AppTextField(value = form.city, onValueChange = { form = form.copy(city = it) }, label = "City")
            AppTextField(value = form.state, onValueChange = { form = form.copy(state = it) }, label = "State")
            AppTextField(value = form.zipCode, onValueChange = { form = form.copy(zipCode = it) }, label = "Zip code")
            AppTextField(value = form.country, onValueChange = { form = form.copy(country = it) }, label = "Country")
        }

        SignupGroup("Security") {
            AppTextField(value = form.password, onValueChange = { form = form.copy(password = it) }, label = "Password", isPassword = true)
            AppTextField(value = form.confirmPassword, onValueChange = { form = form.copy(confirmPassword = it) }, label = "Confirm password", isPassword = true)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
            PrimaryButton(text = "Create account", onClick = { onSubmit(form) })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack) {
                    Text("Back", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onGoToLogin) {
                    Text("Have an account? Login", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SignupGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppCard {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}
