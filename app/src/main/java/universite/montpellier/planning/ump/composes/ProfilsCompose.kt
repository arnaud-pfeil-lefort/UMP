package universite.montpellier.planning.ump.composes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import universite.montpellier.planning.ump.PlanningEtat
import universite.montpellier.planning.ump.data.Profil
import universite.montpellier.planning.ump.ui.theme.couleurMatiere
import java.util.Locale

/** Couleur d'avatar d'un profil, selon sa position dans la liste. */
private fun couleurProfil(profils: List<Profil>, profil: Profil) =
    couleurMatiere(profils.indexOfFirst { it.id == profil.id }.coerceAtLeast(0) * 5)

@Composable
fun AvatarProfil(nom: String, couleur: Color, taille: Dp = 32.dp) {
    Box(
        Modifier
            .size(taille)
            .clip(CircleShape)
            .background(couleur),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            nom.trim().take(1).uppercase(Locale.FRENCH).ifEmpty { "?" },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (taille.value * 0.45f).sp,
        )
    }
}

/** Pastille cliquable affichée dans l'en-tête du planning. */
@Composable
fun BadgeProfil(etat: PlanningEtat, onClick: () -> Unit) {
    val profil = etat.profilActif ?: return
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarProfil(profil.nom, couleurProfil(etat.profils, profil), 26.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                profil.nom,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 4.dp),
            )
            Text("▾", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Formulaire nom + lien, utilisé au premier lancement et pour ajouter/modifier un profil. */
@Composable
fun FormulaireProfil(
    nomInitial: String,
    urlInitiale: String,
    nomParDefaut: String,
    texteBouton: String,
    enCours: Boolean,
    erreur: String?,
    onChange: () -> Unit,
    onValider: (nom: String, url: String) -> Unit,
) {
    var nom by remember { mutableStateOf(nomInitial) }
    var url by remember { mutableStateOf(urlInitiale) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = nom,
            onValueChange = { nom = it },
            label = { Text("Nom du profil") },
            placeholder = { Text(nomParDefaut) },
            singleLine = true,
            enabled = !enCours,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it; onChange() },
            label = { Text("Lien iCal ADE") },
            placeholder = { Text("https://proseconsult.umontpellier.fr/…") },
            maxLines = 4,
            enabled = !enCours,
            isError = erreur != null,
            supportingText = erreur?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onValider(nom, url) },
            enabled = !enCours && url.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (enCours) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(12.dp))
                Text("Vérification du lien…")
            } else {
                Text(texteBouton)
            }
        }
    }
}

@Composable
private fun AideLien() {
    Text(
        "Dans ADE, utilise l'export de l'agenda pour générer un lien iCal (.ics), puis colle-le ici.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Écran affiché au tout premier lancement, quand aucun profil n'existe. */
@Composable
fun EcranBienvenue(etat: PlanningEtat, onValider: (String, String) -> Unit, onChange: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("📅", fontSize = 56.sp)
            Text("Bienvenue !", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "Colle le lien iCal de ton emploi du temps ADE pour commencer. Il sera enregistré dans ton premier profil, et tu pourras ajouter d'autres plannings plus tard.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FormulaireProfil(
                nomInitial = "",
                urlInitiale = "",
                nomParDefaut = "Profil 1",
                texteBouton = "Continuer",
                enCours = etat.formulaireEnCours,
                erreur = etat.erreurFormulaire,
                onChange = onChange,
                onValider = onValider,
            )
            AideLien()
        }
    }
}

private sealed interface ModeFeuille {
    data object Liste : ModeFeuille
    data object Ajout : ModeFeuille
    data class Edition(val profil: Profil) : ModeFeuille
}

/** Feuille de gestion des profils : changer de profil, en ajouter, modifier ou supprimer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeuilleProfils(
    etat: PlanningEtat,
    onFermer: () -> Unit,
    onChoisir: (String) -> Unit,
    onEnregistrer: (id: String?, nom: String, url: String, onSucces: () -> Unit) -> Unit,
    onSupprimer: (String) -> Unit,
    onChangeFormulaire: () -> Unit,
) {
    var mode by remember { mutableStateOf<ModeFeuille>(ModeFeuille.Liste) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onFermer, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val m = mode) {
                ModeFeuille.Liste -> {
                    Text("Profils", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    etat.profils.forEach { profil ->
                        LigneProfil(
                            profil = profil,
                            couleur = couleurProfil(etat.profils, profil),
                            actif = profil.id == etat.profilActif?.id,
                            onChoisir = { onChoisir(profil.id); onFermer() },
                            onModifier = { onChangeFormulaire(); mode = ModeFeuille.Edition(profil) },
                        )
                    }
                    OutlinedButton(
                        onClick = { onChangeFormulaire(); mode = ModeFeuille.Ajout },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("+  Ajouter un profil") }
                }

                ModeFeuille.Ajout -> {
                    EnTeteFormulaire("Nouveau profil") { mode = ModeFeuille.Liste }
                    FormulaireProfil(
                        nomInitial = "",
                        urlInitiale = "",
                        nomParDefaut = "Profil ${etat.profils.size + 1}",
                        texteBouton = "Ajouter",
                        enCours = etat.formulaireEnCours,
                        erreur = etat.erreurFormulaire,
                        onChange = onChangeFormulaire,
                        onValider = { nom, url -> onEnregistrer(null, nom, url) { onFermer() } },
                    )
                    AideLien()
                }

                is ModeFeuille.Edition -> {
                    var confirmerSuppression by remember(m.profil.id) { mutableStateOf(false) }
                    EnTeteFormulaire("Modifier le profil") { mode = ModeFeuille.Liste }
                    key(m.profil.id) { FormulaireProfil(
                        nomInitial = m.profil.nom,
                        urlInitiale = m.profil.url,
                        nomParDefaut = m.profil.nom,
                        texteBouton = "Enregistrer",
                        enCours = etat.formulaireEnCours,
                        erreur = etat.erreurFormulaire,
                        onChange = onChangeFormulaire,
                        onValider = { nom, url -> onEnregistrer(m.profil.id, nom, url) { mode = ModeFeuille.Liste } },
                    ) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TextButton(
                        onClick = {
                            if (confirmerSuppression) {
                                onSupprimer(m.profil.id)
                                mode = ModeFeuille.Liste
                                if (etat.profils.size <= 1) onFermer()
                            } else {
                                confirmerSuppression = true
                            }
                        },
                        enabled = !etat.formulaireEnCours,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (confirmerSuppression) "Confirmer la suppression de « ${m.profil.nom} »" else "Supprimer ce profil",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnTeteFormulaire(titre: String, onRetour: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onRetour),
            contentAlignment = Alignment.Center,
        ) { Text("‹", fontSize = 26.sp) }
        Spacer(Modifier.width(4.dp))
        Text(titre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LigneProfil(
    profil: Profil,
    couleur: Color,
    actif: Boolean,
    onChoisir: () -> Unit,
    onModifier: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (actif) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onChoisir)
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = actif, onClick = onChoisir)
        AvatarProfil(profil.nom, couleur, 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            profil.nom,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onModifier) { Text("Modifier") }
    }
}
