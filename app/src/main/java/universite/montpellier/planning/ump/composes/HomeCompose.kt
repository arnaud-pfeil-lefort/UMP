package universite.montpellier.planning.ump.composes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import universite.montpellier.planning.ump.Cours
import universite.montpellier.planning.ump.PlanningEtat
import universite.montpellier.planning.ump.ui.theme.LigneMaintenant
import universite.montpellier.planning.ump.ui.theme.couleurMatiere
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private const val NB_PAGES = 4001
private const val PAGE_AUJOURDHUI = NB_PAGES / 2
private val HAUTEUR_HEURE = 76.dp
private val LARGEUR_HEURES = 52.dp

private fun String.majuscule() = replaceFirstChar { it.titlecase(Locale.FRENCH) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    etat: PlanningEtat,
    onRafraichir: () -> Unit,
    onIgnorerErreur: () -> Unit,
) {
    val aujourdhui = remember { LocalDate.now() }
    val pagerState = rememberPagerState(initialPage = PAGE_AUJOURDHUI) { NB_PAGES }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var coursSelectionne by remember { mutableStateOf<Cours?>(null) }

    fun dateDePage(page: Int): LocalDate = aujourdhui.plusDays((page - PAGE_AUJOURDHUI).toLong())
    fun allerA(date: LocalDate) {
        val page = PAGE_AUJOURDHUI + ChronoUnit.DAYS.between(aujourdhui, date).toInt()
        scope.launch { pagerState.animateScrollToPage(page.coerceIn(0, NB_PAGES - 1)) }
    }

    val jourAffiche = dateDePage(pagerState.currentPage)

    LaunchedEffect(etat.erreur) {
        val erreur = etat.erreur ?: return@LaunchedEffect
        snackbar.showSnackbar(erreur)
        onIgnorerErreur()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(bottom = padding.calculateBottomPadding()).fillMaxSize()) {
            EnTete(
                jour = jourAffiche,
                aujourdhui = aujourdhui,
                coursParJour = etat.coursParJour,
                rafraichissement = etat.rafraichissement,
                onChoisirJour = ::allerA,
            )

            if (etat.chargementInitial) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                PullToRefreshBox(
                    isRefreshing = etat.rafraichissement,
                    onRefresh = onRafraichir,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                        val date = dateDePage(page)
                        PageJour(
                            date = date,
                            cours = etat.coursParJour[date].orEmpty(),
                            estAujourdhui = date == aujourdhui,
                            miseAJour = etat.miseAJour,
                            onCours = { coursSelectionne = it },
                        )
                    }
                }
            }
        }
    }

    coursSelectionne?.let { DetailsCours(it, onFermer = { coursSelectionne = null }) }
}

/* ------------------------------------------------------------------------------------------ */
/*  En-tête : date, bouton « Aujourd'hui » et bandeau de la semaine                            */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun EnTete(
    jour: LocalDate,
    aujourdhui: LocalDate,
    coursParJour: Map<LocalDate, List<Cours>>,
    rafraichissement: Boolean,
    onChoisirJour: (LocalDate) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 2.dp) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH).format(jour).majuscule(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        DateTimeFormatter.ofPattern("EEEE d", Locale.FRENCH).format(jour).majuscule(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                AnimatedVisibility(visible = jour != aujourdhui) {
                    FilledTonalButton(onClick = { onChoisirJour(aujourdhui) }) { Text("Aujourd'hui") }
                }
            }

            BandeauSemaine(jour, aujourdhui, coursParJour, onChoisirJour)

            Box(Modifier.fillMaxWidth().height(3.dp)) {
                if (rafraichissement) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BandeauSemaine(
    jour: LocalDate,
    aujourdhui: LocalDate,
    coursParJour: Map<LocalDate, List<Cours>>,
    onChoisirJour: (LocalDate) -> Unit,
) {
    val lundi = jour.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoutonFleche("‹") { onChoisirJour(jour.minusWeeks(1)) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0L until 7L) {
                val date = lundi.plusDays(i)
                CelluleJour(
                    date = date,
                    selectionne = date == jour,
                    estAujourdhui = date == aujourdhui,
                    cours = coursParJour[date].orEmpty(),
                    onClick = { onChoisirJour(date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        BoutonFleche("›") { onChoisirJour(jour.plusWeeks(1)) }
    }
}

@Composable
private fun BoutonFleche(symbole: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbole, fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CelluleJour(
    date: LocalDate,
    selectionne: Boolean,
    estAujourdhui: Boolean,
    cours: List<Cours>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val couleurs = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            DateTimeFormatter.ofPattern("EEE", Locale.FRENCH).format(date).take(3).majuscule(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selectionne) couleurs.primary else couleurs.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selectionne) couleurs.primary else Color.Transparent)
                .then(
                    if (estAujourdhui && !selectionne) Modifier.border(1.5.dp, couleurs.primary, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selectionne || estAujourdhui) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    selectionne -> couleurs.onPrimary
                    estAujourdhui -> couleurs.primary
                    else -> couleurs.onSurface
                },
            )
        }
        Spacer(Modifier.height(5.dp))
        // Une pastille par matière du jour (3 max), dans la couleur de la matière.
        Row(Modifier.height(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            cours.map { it.couleur }.distinct().take(3).forEach { c ->
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(couleurMatiere(c))
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------ */
/*  Page d'une journée : résumé + grille horaire                                              */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun PageJour(
    date: LocalDate,
    cours: List<Cours>,
    estAujourdhui: Boolean,
    miseAJour: Instant?,
    onCours: (Cours) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Resume(cours, miseAJour)
        if (cours.isEmpty()) {
            JourLibre(date)
        } else {
            GrilleHoraire(cours, estAujourdhui, onCours)
        }
    }
}

@Composable
private fun Resume(cours: List<Cours>, miseAJour: Instant?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val texte = if (cours.isEmpty()) "Aucun cours" else {
            val debut = cours.minOf { it.dateDebut }
            val fin = cours.maxOf { it.dateFin }
            "${cours.size} cours  ·  ${debut.heureCourte()} – ${fin.heureCourte()}"
        }
        Text(
            texte,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (miseAJour != null) {
            val maj = LocalDateTime.ofInstant(miseAJour, ZoneId.systemDefault())
            val texteMaj = if (maj.toLocalDate() == LocalDate.now()) "Mis à jour à ${maj.heureCourte()}"
            else "Mis à jour le ${DateTimeFormatter.ofPattern("d/MM", Locale.FRENCH).format(maj)}"
            Text(texteMaj, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun JourLibre(date: LocalDate) {
    val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 96.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (weekend) "🌴" else "☕", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            if (weekend) "C'est le week-end !" else "Journée libre",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Aucun cours prévu ce jour-là.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Position d'un cours dans la grille : colonne occupée parmi les cours qui se chevauchent. */
private data class Placement(val cours: Cours, val colonne: Int, val nbColonnes: Int)

/** Répartit les cours qui se chevauchent sur plusieurs colonnes côte à côte. */
private fun disposer(cours: List<Cours>): List<Placement> {
    val resultat = mutableListOf<Placement>()
    val groupe = mutableListOf<Pair<Cours, Int>>()
    val finsColonnes = mutableListOf<LocalDateTime>()
    var finGroupe: LocalDateTime? = null

    fun cloreGroupe() {
        groupe.forEach { (c, col) -> resultat += Placement(c, col, finsColonnes.size) }
        groupe.clear()
        finsColonnes.clear()
    }

    for (c in cours.sortedWith(compareBy({ it.dateDebut }, { it.dateFin }))) {
        if (finGroupe != null && !c.dateDebut.isBefore(finGroupe)) cloreGroupe()
        val col = finsColonnes.indexOfFirst { !it.isAfter(c.dateDebut) }.takeIf { it >= 0 } ?: finsColonnes.size
        if (col == finsColonnes.size) finsColonnes += c.dateFin else finsColonnes[col] = c.dateFin
        groupe += c to col
        finGroupe = if (finGroupe == null || c.dateFin.isAfter(finGroupe)) c.dateFin else finGroupe
    }
    cloreGroupe()
    return resultat
}

@Composable
private fun GrilleHoraire(cours: List<Cours>, estAujourdhui: Boolean, onCours: (Cours) -> Unit) {
    val heureDebut = minOf(8, cours.minOf { it.dateDebut.hour })
    val heureFin = maxOf(18, cours.maxOf { if (it.dateFin.minute > 0) it.dateFin.hour + 1 else it.dateFin.hour })
        .coerceAtMost(24)
    val couleurs = MaterialTheme.colorScheme

    fun position(t: LocalDateTime) = HAUTEUR_HEURE * ((t.hour - heureDebut) + t.minute / 60f)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(HAUTEUR_HEURE * (heureFin - heureDebut) + 16.dp)
            .padding(top = 8.dp, end = 16.dp)
    ) {
        val largeurCours = maxWidth - LARGEUR_HEURES - 8.dp

        // Lignes et libellés des heures
        for (h in heureDebut..heureFin) {
            Row(
                Modifier.offset(y = HAUTEUR_HEURE * (h - heureDebut) - 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "%02d:00".format(h),
                    modifier = Modifier.width(LARGEUR_HEURES).padding(end = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = couleurs.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
                HorizontalDivider(color = couleurs.outlineVariant)
            }
        }

        // Cours
        for (p in disposer(cours)) {
            val largeur = largeurCours / p.nbColonnes
            val haut = position(p.cours.dateDebut)
            val hauteur = (position(p.cours.dateFin) - haut).coerceAtLeast(28.dp)
            CoursCompose(
                cours = p.cours,
                hauteur = hauteur,
                onClick = { onCours(p.cours) },
                modifier = Modifier
                    .offset(x = LARGEUR_HEURES + 8.dp + largeur * p.colonne, y = haut + 1.dp)
                    .width(largeur - 4.dp)
                    .height(hauteur - 2.dp),
            )
        }

        // Ligne « maintenant »
        if (estAujourdhui) {
            var maintenant by remember { mutableStateOf(LocalDateTime.now()) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(30_000)
                    maintenant = LocalDateTime.now()
                }
            }
            if (maintenant.hour in heureDebut until heureFin) {
                Row(
                    Modifier.offset(x = LARGEUR_HEURES + 2.dp, y = position(maintenant) - 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(LigneMaintenant))
                    Box(Modifier.fillMaxWidth().height(2.dp).background(LigneMaintenant))
                }
            }
        }
    }
}
