package cz.janek.vineyardlog.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverCropTest {
    @Test fun flatCreditScalesWithTheMixAndTheAreaSown() {
        // whole block, pure legume mix: the flat Ontario credit
        assertEquals(45.0, CoverCrop.creditKgNPerHa(), 1e-9)
        // half the seed is legume, sown in every second inter-row of a block that is 70 % inter-row
        assertEquals(45.0 * 0.5 * 0.35, CoverCrop.creditKgNPerHa(legumeShare = 0.5, sownShare = 0.35), 1e-9)
        assertEquals(0.0, CoverCrop.creditKgNPerHa(legumeShare = 0.0), 1e-9)
        assertEquals(0.0, CoverCrop.creditKgNPerHa(sownShare = 0.0), 1e-9)
    }

    @Test fun weighedBiomassBeatsTheFlatCredit() {
        // 6 t/ha of dry matter at 2.5 % N = 150 kg N, of which 45 % reaches the vine
        assertEquals(150.0 * 0.45, CoverCrop.creditKgNPerHa(dryMatterTHa = 6.0, nPercent = 2.5), 1e-9)
        // on a third of the block it is a third of that
        assertEquals(150.0 * 0.45 / 3, CoverCrop.creditKgNPerHa(sownShare = 1.0 / 3, dryMatterTHa = 6.0, nPercent = 2.5), 1e-6)
    }

    @Test fun tooLittleBiomassIsNoCredit() {
        assertEquals(0.0, CoverCrop.creditKgNPerHa(dryMatterTHa = 1.5, nPercent = 3.0), 1e-9)
        assertEquals(0.0, CoverCrop.creditKgNPerHa(dryMatterTHa = CoverCrop.MIN_BIOMASS_T_HA - 0.01, nPercent = 3.0), 1e-9)
    }

    @Test fun sharesAreClamped() {
        assertEquals(45.0, CoverCrop.creditKgNPerHa(legumeShare = 2.0, sownShare = 5.0), 1e-9)
    }
}
