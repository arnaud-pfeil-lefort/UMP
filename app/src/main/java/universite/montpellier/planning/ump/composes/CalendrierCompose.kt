package universite.montpellier.planning.ump.composes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import universite.montpellier.planning.ump.PlanningEtat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private const val NB_PAGES_CAL = 1001
private const val PAGE_CAL_ACTUELLE = NB_PAGES_CAL / 2
private val INITIALES_JOURS = listOf("L", "M", "M", "J", "V", "S", "D")

private fun String.majuscule() = replaceFirstChar { it.titlecase(Locale.FRENCH) }
private fun LocalDate.lundi(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Teinte de fond des jours où il y a cours. */
@Composable
private fun fondJourAvecCours(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

/**
 * En-tête du planning :
 *  - le mois (touchable pour déplier le calendrier du mois) et le profil,
 *  - le jour affiché + bouton « Aujourd'hui »,
 *  - la semaine, que l'on fait défiler au doigt (ou le mois entier quand il est déplié).
 */
@Composable
fun EnTete(
    etat: PlanningEtat,
    jour: LocalDate,
    aujourdhui: LocalDate,
    onChoisirJour: (LocalDate) -> Unit,
    onOuvrirProfils: () -> Unit,
) {
    var moisDeplie by rememberSaveable { mutableStateOf(false) }
    val joursAvecCours = remember(etat.coursParJour) { etat.coursParJour.filterValues { it.isNotEmpty() }.keys }
    var moisVisible by remember { mutableStateOf(YearMonth.from(jour)) }
    val moisTitre = if (moisDeplie) moisVisible else YearMonth.from(jour)

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 2.dp) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelecteurMois(
                    mois = moisTitre,
                    deplie = moisDeplie,
                    onClick = {
                        moisVisible = YearMonth.from(jour)
                        moisDeplie = !moisDeplie
                    },
                )
                Spacer(Modifier.weight(1f))
                BadgeProfil(etat, onOuvrirProfils)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    DateTimeFormatter.ofPattern("EEEE d", Locale.FRENCH).format(jour).majuscule(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(visible = jour != aujourdhui, enter = fadeIn(), exit = fadeOut()) {
                    FilledTonalButton(onClick = { onChoisirJour(aujourdhui) }) { Text("Aujourd'hui") }
                }
            }

            Spacer(Modifier.height(10.dp))

            AnimatedContent(
                targetState = moisDeplie,
                transitionSpec = {
                    (fadeIn() + expandVertically(expandFrom = Alignment.Top)) togetherWith
                        (fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top))
                },
                label = "calendrier",
            ) { deplie ->
                if (deplie) {
                    CalendrierMois(
                        jour = jour,
                        aujourdhui = aujourdhui,
                        joursAvecCours = joursAvecCours,
                        onMoisVisible = { moisVisible = it },
                        onChoisirJour = {
                            onChoisirJour(it)
                            moisDeplie = false
                        },
                    )
                } else {
                    BandeauSemaine(jour, aujourdhui, joursAvecCours, onChoisirJour)
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            ) {
                if (etat.rafraichissement) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SelecteurMois(mois: YearMonth, deplie: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (deplie) 180f else 0f, label = "chevron")
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH).format(mois).majuscule(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "▾",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.rotate(rotation),
        )
    }
}

/**
 * Synchronise un pager de calendrier (semaines ou mois) avec le jour affiché :
 *  - quand le jour change ailleurs (glissement des journées, « Aujourd'hui »…), le pager suit ;
 *  - quand l'utilisateur fait glisser le pager lui-même, [onGlisse] reçoit la nouvelle page.
 */
@Composable
private fun SynchroniserPager(
    state: PagerState,
    pageCible: Int,
    onGlisse: (Int) -> Unit,
) {
    val glisse by state.interactionSource.collectIsDraggedAsState()
    var parUtilisateur by remember { mutableStateOf(false) }
    val onGlisseActuel by rememberUpdatedState(onGlisse)

    LaunchedEffect(glisse) { if (glisse) parUtilisateur = true }

    LaunchedEffect(pageCible) {
        if (!parUtilisateur && state.currentPage != pageCible) state.animateScrollToPage(pageCible)
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .collect {
                if (parUtilisateur) {
                    parUtilisateur = false
                    onGlisseActuel(state.currentPage)
                }
            }
    }
}

/* ------------------------------------------------------------------------------------------ */
/*  Vue semaine                                                                               */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun BandeauSemaine(
    jour: LocalDate,
    aujourdhui: LocalDate,
    joursAvecCours: Set<LocalDate>,
    onChoisirJour: (LocalDate) -> Unit,
) {
    val lundiRef = remember(aujourdhui) { aujourdhui.lundi() }
    fun lundiDePage(page: Int) = lundiRef.plusWeeks((page - PAGE_CAL_ACTUELLE).toLong())
    fun pageDe(date: LocalDate) = PAGE_CAL_ACTUELLE + ChronoUnit.WEEKS.between(lundiRef, date.lundi()).toInt()

    val state = rememberPagerState(initialPage = pageDe(jour)) { NB_PAGES_CAL }
    val jourActuel by rememberUpdatedState(jour)

    // Glisser la semaine garde le même jour de la semaine (mardi → mardi suivant).
    SynchroniserPager(state, pageDe(jour)) { page ->
        if (page != pageDe(jourActuel)) {
            onChoisirJour(lundiDePage(page).plusDays((jourActuel.dayOfWeek.value - 1).toLong()))
        }
    }

    HorizontalPager(state = state, modifier = Modifier.fillMaxWidth()) { page ->
        val lundi = lundiDePage(page)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            for (i in 0 until 7) {
                val date = lundi.plusDays(i.toLong())
                CelluleSemaine(
                    date = date,
                    initiale = INITIALES_JOURS[i],
                    selectionne = date == jour,
                    estAujourdhui = date == aujourdhui,
                    aCours = date in joursAvecCours,
                    onClick = { onChoisirJour(date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Un jour de la semaine : une « pilule » verticale avec l'initiale et le numéro du jour.
 * Jour avec cours : pilule teintée et chiffre net. Jour sans cours : pas de fond, chiffre estompé.
 * Aujourd'hui : contour de la couleur principale.
 */
@Composable
private fun CelluleSemaine(
    date: LocalDate,
    initiale: String,
    selectionne: Boolean,
    estAujourdhui: Boolean,
    aCours: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val couleurs = MaterialTheme.colorScheme
    val forme = RoundedCornerShape(22.dp)
    val fond = when {
        selectionne -> couleurs.primary
        aCours -> fondJourAvecCours()
        else -> Color.Transparent
    }
    val texte = when {
        selectionne -> couleurs.onPrimary
        aCours -> couleurs.onSurface
        else -> couleurs.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .width(44.dp)
                .clip(forme)
                .background(fond)
                .then(if (estAujourdhui && !selectionne) Modifier.border(1.5.dp, couleurs.primary, forme) else Modifier)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                initiale,
                style = MaterialTheme.typography.labelSmall,
                color = if (selectionne) texte.copy(alpha = 0.8f) else texte.copy(alpha = texte.alpha * 0.7f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selectionne || estAujourdhui || aCours) FontWeight.Bold else FontWeight.Normal,
                color = if (estAujourdhui && !selectionne) couleurs.primary else texte,
            )
        }
    }
}

/* ------------------------------------------------------------------------------------------ */
/*  Vue mois                                                                                  */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun CalendrierMois(
    jour: LocalDate,
    aujourdhui: LocalDate,
    joursAvecCours: Set<LocalDate>,
    onMoisVisible: (YearMonth) -> Unit,
    onChoisirJour: (LocalDate) -> Unit,
) {
    val moisRef = remember(aujourdhui) { YearMonth.from(aujourdhui) }
    fun moisDePage(page: Int) = moisRef.plusMonths((page - PAGE_CAL_ACTUELLE).toLong())
    fun pageDe(date: LocalDate) = PAGE_CAL_ACTUELLE + ChronoUnit.MONTHS.between(moisRef, YearMonth.from(date)).toInt()

    val state = rememberPagerState(initialPage = pageDe(jour)) { NB_PAGES_CAL }
    // Dans la vue mois, glisser change seulement le mois consulté, pas le jour affiché.
    SynchroniserPager(state, pageDe(jour)) { page -> onMoisVisible(moisDePage(page)) }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            INITIALES_JOURS.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        HorizontalPager(state = state, modifier = Modifier.fillMaxWidth()) { page ->
            val mois = moisDePage(page)
            val premierLundi = mois.atDay(1).lundi()
            // Toujours 6 lignes pour que la hauteur ne saute pas d'un mois à l'autre.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                for (semaine in 0 until 6) {
                    Row(Modifier.fillMaxWidth()) {
                        for (j in 0 until 7) {
                            val date = premierLundi.plusDays((semaine * 7 + j).toLong())
                            CelluleMois(
                                date = date,
                                dansLeMois = YearMonth.from(date) == mois,
                                selectionne = date == jour,
                                estAujourdhui = date == aujourdhui,
                                aCours = date in joursAvecCours,
                                onClick = { onChoisirJour(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelluleMois(
    date: LocalDate,
    dansLeMois: Boolean,
    selectionne: Boolean,
    estAujourdhui: Boolean,
    aCours: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val couleurs = MaterialTheme.colorScheme
    val fond = when {
        selectionne -> couleurs.primary
        aCours -> fondJourAvecCours()
        else -> Color.Transparent
    }
    Column(
        modifier
            .height(40.dp)
            .alpha(if (dansLeMois) 1f else 0.35f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(fond)
                .then(if (estAujourdhui && !selectionne) Modifier.border(1.5.dp, couleurs.primary, CircleShape) else Modifier)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selectionne || estAujourdhui || aCours) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    selectionne -> couleurs.onPrimary
                    estAujourdhui -> couleurs.primary
                    aCours -> couleurs.onSurface
                    else -> couleurs.onSurfaceVariant.copy(alpha = 0.5f)
                },
            )
        }
    }
}
