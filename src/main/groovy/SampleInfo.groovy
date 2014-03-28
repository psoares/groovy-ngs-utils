// vim: ts=4:sw=4:expandtab:cindent:
/*
 *  Groovy NGS Utils - Some simple utilites for processing Next Generation Sequencing data.
 *
 *  Copyright (C) 2014 Simon Sadedin, ssadedin<at>gmail.com
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
import java.text.ParseException;

enum Sex {
	MALE, FEMALE, OTHER	
	
	static Sex decode(String value) {
		if(!value?.trim())
			FEMALE
		else
		if(value == "1")
			MALE
		else
		if(value == "2")
			FEMALE
		else
			throw new IllegalArgumentException("Bad sex value [$value] specified")
	}
}

enum SampleType {
	NORMAL, TUMOR
	
	static Consanguinity decode(String value) {
		if(!value?.trim())
			NORMAL
		if(value == "1")
			NORMAL
		else
		if(value == "2")
			TUMOR
		else
			throw new IllegalArgumentException("Bad sample type value [$value] specified")
	}
}

enum Consanguinity {
	NOT_CONSANGUINOUS, CONSANGUINOUS, SUSPECTED, UNKNOWN
	
	static Consanguinity decode(String value) {
		if(!value?.trim())
			NOT_CONSANGUINOUS
		if(value == "0")
			NOT_CONSANGUINOUS
		else
		if(value == "1")
			CONSANGUINOUS
		else
		if(value == "2")
			SUSPECTED
		else
		if(value == "3" || value == "8")
			UNKNOWN
		else
			throw new IllegalArgumentException("Bad consanguinity value [$value] specified")
	}
}

enum Ethnicity {
	UNKNOWN, EUROPEAN, AFRICAN
	
	static Ethnicity decode(String value) {
		if(!value?.trim())
			return UNKNOWN
			
		switch(value.trim()) {
			case "0":
				UNKNOWN
				break;
			case "1":
				EUROPEAN
				break;
			case "2":
				AFRICAN
				break
			default:
				throw new IllegalArgumentException("Bad Ethnicity value [$value] specified")
		}
	}
}

/**
 * Meta data about a sample.
 * <p>
 * Designed to be compatible with the MGHA sample information format.
 */
class SampleInfo {

    /** Sample name */
    String  sample

    /** 
     * List of files containing data specific to this sample, indexed by file types:
     *  - fastq
     *  - coverage (output from coverageBed)
     *  - vcf
     *  - bam
     */
    Map    files = new Hashtable() // thread safe
	
	def columns = ["Sample_ID","Batch","Cohort","Fastq_Files","Prioritised_Genes","Sex","Sample_Type","Consanguinity","Variants_File","Pedigree_File","Ethnicity","VariantCall_Group","DNA_Concentration","DNA_Quantity","DNA_Quality","DNA_Date","Capture_Date","Sequencing_Date","Mean_Coverage","Duplicate_Percentage","Machine_ID"," Hospital_Centre","Sequencing_Contact","Pipeline_Contact"]
	
	/** Id of batch in which the sample was sequenced */
	String batch
	
	/** Whether the sample type is normal or tumor */
	SampleType sampleType = SampleType.NORMAL
	
	/** Whether the sample is consanguinous */
	Consanguinity consanguinity = Consanguinity.NOT_CONSANGUINOUS

    /** Target (flagship) name */
    String  target

    /** List of genes prioritised for the sample */
    Map<String,Integer>    geneCategories

    /** The library */
    String library

    /** The sex of the sample */
    Sex sex
	
	Ethnicity ethnicity
	
    /** The pedigree of the family */
    String pedigree
	
	/** DNA quality in nanograms */
	float dnaConcentrationNg
	
	float dnaQuality
	
	float dnaQuantity
	
	List<Date> dnaDates
	
	List<Date> captureDates
	
	List<Date> sequencingDates
	
	/** Mean coverage as reported by sequencing provider */
	float meanCoverage
	
	/** Hospital or organization responsible for the patient from which the sample originated */
	String instituion
	
	List<String> machineIds
	
	String sequencingContact
	
	String analysisContact

    Map<String,String> fileMappings = [
        bam : "bam",
        fastq: "fastq.gz",
        coverage: "exoncoverage.txt",
        vcf : "vcf"
    ]

    void indexFileTypes() {
        fileMappings.each { index, ending -> indexFileType(index,ending) }
    }

    void indexFileType(String index, String ending) {
        def matchingFiles  = files.all.grep { it.endsWith(ending) }
        if(matchingFiles)
            this.files[index] = matchingFiles
    }

    /**
     * Parse the given file to extract sample information
     *
     * @return  List of (Map) objects defining properties of each
     *          sample: 
     *              <li>sample name (sample), 
     *              <li>FastQ files (files), 
     *              <li>Name of flagship (target)
     *              <li>Genes to be classed as high priority (genes)
     */
    static parse_sample_info(fileName) {
		
        def lines = new File(fileName).readLines().grep { 
			!it.trim().startsWith('#') && // ignore comment lines
			!it.trim().toLowerCase().startsWith("sample_id") && // ignore header line, if it is present
			it.trim() // ignore completely blank lines
		}
		
		def columns = ["Sample_ID","Batch","Cohort","Fastq_Files","Prioritised_Genes","Sex","Sample_Type","Consanguinity","Variants_File","Pedigree_File","Ethnicity","VariantCall_Group","DNA_Concentration","DNA_Quantity","DNA_Quality","DNA_Date","Capture_Date","Sequencing_Date","Mean_Coverage","Duplicate_Percentage","Machine_ID"," Hospital_Centre","Sequencing_Contact","Pipeline_Contact"]
		
		println "Parsing lines ${lines.join('\n')}"
		
		int lineCount = 0
        def sample_info = new TSV(new StringReader(lines.join("\n")), columns).collect { fields ->
				println "Found sample " + fields.Sample_ID
			
				try {
	                def si = new SampleInfo(
	                    sample: fields.Sample_ID,
	                    target: fields.Cohort,
	                    geneCategories:  [:],
						batch: fields.Batch,
	                    pedigree: fields.Pedigree_File               
					)
					
					si.sex = Sex.decode(fields.Sex) 
					si.consanguinity = Consanguinity.decode(fields.Consanguinity)
					si.ethnicity  = Ethnicity.decode(fields.Ethnicity)
					si.dnaDates = fields.DNA_Date?.split(",")*.trim().collect { parseDate(it) }
					si.captureDates = fields.Capture_Date?.split(",")*.trim().collect { parseDate(it) }
					si.sequencingDates = fields.Sequencing_Date?.split(",")*.trim().collect { parseDate(it) }
					si.dnaConcentrationNg = fields.DNA_Concentration?.toFloat()
					si.dnaQuality = fields.DNA_Quality?.toFloat()
					si.dnaQuantity = fields.DNA_Quantity?.toFloat()
					si.meanCoverage = fields.Mean_Coverage?.toFloat()
					si.machineIds = fields.Machine_ID?.split(",")*.trim() as List
					si.sequencingContact = fields.Sequencing_Contact
					si.analysisContact = fields.Pipeline_Contact
					
	                si.files.all = fields.Fastq_Files.split(",")*.trim().collect {new File(it).parentFile?it:"../data/$it"}
	                si.indexFileTypes()
	
	                // Index category to gene
	                if(fields.Prioritised_Genes) {
	                    def genes = fields.Prioritised_Genes.split(" ")*.split(":").collect { 
							// HACK: Excel is exporting character -96 here, not sure why!
							int category = new String(it[0].bytes.grep { it != -96 } as byte[]).trim().toInteger()
	                        [ category, /* genes */ it[1].split(",")*.trim() ]
	                    }.collectEntries()
	
	                    // Invert 
	                    genes.each { k,v -> v.each { si.geneCategories[it] = k } }
	                }
	
					++lineCount
	                return si
				}
				catch(Exception e) {
					throw new RuntimeException("Error parsing meta data for sample ${fields.Sample_ID} on line $lineCount", e)
				}
        }.collectEntries { [it.sample, it] } // Convert to map keyed by sample name
    }
    
    String toTsv() {
        [sample, target, files.collect { it.key == "all" ? [] : it.value }.flatten().join(","), geneCategories.collect { it.key + ":" + it.value.join(",") }.join(" "), sex].join("\t")
    }

    String toString() {
        "$sample($sex)"
    }
	
	static Date parseDate(String dateValue) {
		if(dateValue == null)
			return null
		return Date.parse("yyyyMMdd", dateValue)
	}
}
