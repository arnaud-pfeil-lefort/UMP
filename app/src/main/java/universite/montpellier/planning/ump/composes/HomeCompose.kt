package universite.montpellier.planning.ump.composes

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import universite.montpellier.planning.ump.Cours
import universite.montpellier.planning.ump.PlanningEtat
import universite.montpellier.planning.ump.ui.theme.LigneMaintenant
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

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
    onOuvrirProfils: () -> Unit,
) {
    val aujourdhui = remember { LocalDate.now() }
    val pagerState = rememberPagerState(initialPage = PAGE_AUJOURDHUI) { NB_PAGES }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var coursSelectionne by remember { mutableStateOf<Cours?>(null) }

    fun dateDePage(page: Int): LocalDate = aujourdhui.plusDays((page - PAGE_AUJOURDHUI).toLong())
    fun allerA(date: LocalDate) {
        val page = (PAGE_AUJOURDHUI + ChronoUnit.DAYS.between(aujourdhui, date).toInt()).coerceIn(0, NB_PAGES - 1)
        scope.launch {
            // Jour voisin : petite animation. Plus loin : saut direct, sans faire défiler
            // (et donc sélectionner) tous les jours intermédiaires.
            if (abs(page - pagerState.currentPage) <= 1) pagerState.animateScrollToPage(page)
            else pagerState.scrollToPage(page)
        }
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
                etat = etat,
                jour = jourAffiche,
                aujourdhui = aujourdhui,
                onChoisirJour = ::allerA,
                onOuvrirProfils = onOuvrirProfils,
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
