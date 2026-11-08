package universite.montpellier.planning.ump.ui.theme

import androidx.compose.ui.graphics.Color

// Thème clair
val Primaire = Color(0xFF3D5AFE)
val PrimaireConteneur = Color(0xFFE3E7FF)
val SurPrimaireConteneur = Color(0xFF0B1A66)
val Fond = Color(0xFFF7F8FC)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariante = Color(0xFFEEF0F7)
val TexteSecondaire = Color(0xFF5E6273)
val Contour = Color(0xFFDCDFEA)

// Thème sombre
val PrimaireSombre = Color(0xFFAFBBFF)
val PrimaireConteneurSombre = Color(0xFF26337A)
val SurPrimaireConteneurSombre = Color(0xFFE3E7FF)
val FondSombre = Color(0xFF111318)
val SurfaceSombre = Color(0xFF181B22)
val SurfaceVarianteSombre = Color(0xFF232733)
val TexteSecondaireSombre = Color(0xFFA9ADBD)
val ContourSombre = Color(0xFF2E3341)

val LigneMaintenant = Color(0xFFE53935)

/** Une couleur par matière. Les teintes sont choisies pour rester lisibles en thème clair comme sombre. */
val CouleursMatieres = listOf(
    Color(0xFF4F6BED), // indigo
    Color(0xFFE8505B), // corail
    Color(0xFF22A06B), // vert
    Color(0xFFF08C2E), // orange
    Color(0xFF9B5DE5), // violet
    Color(0xFF1C9CC4), // cyan
    Color(0xFFD6458D), // rose
    Color(0xFFB8952A), // moutarde
    Color(0xFF00897B), // sarcelle
    Color(0xFF8D6E63), // brun
    Color(0xFF5C7CFA), // bleuet
    Color(0xFF7CB342), // vert pomme
)

fun couleurMatiere(index: Int): Color = CouleursMatieres[Math.floorMod(index, CouleursMatieres.size)]
