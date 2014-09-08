import static org.junit.Assert.*;

import org.junit.Test;

class PedigreeTest {

    @Test
    public void testParse() {
        def pedigrees = Pedigrees.parse("/Users/simon/work/seth/samples.ped").families
        
        assert pedigrees["J00019"]
        
        println "pedigree J00019D = " + pedigrees["J00019D"]
        
        assert !pedigrees["J00019D"]
        
        assert pedigrees["J00013"].samples.contains("J00013D")
        
        
        println "Samples = " + pedigrees["J00013"].individuals*.id
        println "Affected = "  + pedigrees["J00013"].individuals*.phenoTypes
        
        assert pedigrees["J00013"].affected.contains("J00013")
        assert !pedigrees["J00013"].affected.contains("J00013D")
        
    }
    
    @Test
    void testFindMaximalUnrelatedSamples() {
        def pedigrees = Pedigrees.parse("tests/test.ped")
        
        println "There are ${pedigrees.families.size()} families"
        
        Pedigree fam1 = pedigrees.families["Q00013"]
        def unset = fam1.findMaximalUnrelatedSet()
        
        println "Unrelated set for $fam1.id = $unset"
        
        // Lets find the whole set from all the pedigrees
        def unsetAll = pedigrees.findMaximalUnrelatedSet("Q00013")
        
        println "Unrelated set for Q00013 = $unsetAll"
    }

}
