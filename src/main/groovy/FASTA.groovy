/*
 *  Groovy NGS Utils - Some simple utilites for processing Next Generation Sequencing data.
 *
 *  Copyright (C) 2013 Simon Sadedin, ssadedin<at>gmail.com
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

import gngs.ProgressCounter
import gngs.Regions
import groovy.transform.CompileStatic;
import groovy.transform.Memoized
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;

/**
 * Simple utilities for manipulating FASTA files.
 * <p>
 * <i>Note: only indexed files are supported</i>
 * <p>The core function for this class is to make it easy to read
 * index FASTA files and retrieve sequences of bases corresponding to
 * regions of the genome:
 * <pre>
 * println "The bases at chr1:1000-2000 are " + 
 *      new FASTA("test.fa").basesAt("chr1", 1000,2000)
 * </pre>
 * The above will return the bases as a String object which is 
 * inefficient: firstly because the internal representation is bytes, so a
 * conversion must run to turn those into a String object, and secondly because
 * Strings use wide chars (16 bits) for each character. So if you care a lot about
 * efficiency you should use the byte version:
 * <pre>
 * println "The bases at chr1:1000-2000 are " + 
 *      new FASTA("test.fa").basesBytesAt("chr1", 1000,2000)
 * </pre>
 * 
 * 
 * @author simon.sadedin@mcri.edu.au
 */
class FASTA {
   
    /**
     * The internal (Picard) index for the FASTA file.
     */
    IndexedFastaSequenceFile indexedFastaFile
    
    FASTA() {
        
    }
    
    /**
     * Create a FASTA object for the given FASTA file, which must be indexed.
     * @param fastaFile
     */
    FASTA(String fastaFile) {
        this(new File(fastaFile))
    }
    
    /**
     * Create a FASTA object for the given FASTA file, which must be indexed.
     * @param fastaFile
     */
    FASTA(File fastaFile) {
        this.indexedFastaFile  = new IndexedFastaSequenceFile(fastaFile)
    }
    
    /**
     * Returns the sequence of bases over the given range
     * <b>NOTE:</b>The range is <i>inclusive</i>.
     * 
     * @param contig    contig to query
     * @param start     start position (inclusive)
     * @param end       end position (inclusive)
     * @return
     */
    @Memoized(maxCacheSize = 100)
    String basesAt(String contig, long start, long end) {
      return new String(this.indexedFastaFile.getSubsequenceAt(contig, start, end).bases)
    }
    
    /**
     * Returns the sequence of bases over the given range
     * <b>NOTE:</b>The range is <i>inclusive</i>.
     * 
     * @param contig    contig to query
     * @param start     start position (inclusive)
     * @param end       end position (inclusive)
     * @return  bytes representing the bases in the range, corresponding to ascii codes
     *          of DNA symbols (A = 65, etc.)
     */
    byte[] baseBytesAt(String contig, long start, long end) {
      return this.indexedFastaFile.getSubsequenceAt(contig, start, end).bases
    }
     
    public static final int T = (int)"T".charAt(0)
    public static final int A = (int)"A".charAt(0)
    public static final int C = (int)"C".charAt(0)
    public static final int G = (int)"G".charAt(0)
    public static final int R = (int)"R".charAt(0)
    public static final int Y = (int)"Y".charAt(0)
    public static final int W = (int)"W".charAt(0)
    public static final int K = (int)"K".charAt(0)
    public static final int M = (int)"M".charAt(0)
    public static final int S = (int)"S".charAt(0)
    public static final int H = (int)"H".charAt(0)
    public static final int D = (int)"D".charAt(0)
    public static final int B = (int)"B".charAt(0)
    public static final int N = (int)"N".charAt(0)
    public static final int V = (int)"V".charAt(0)
    
    /**
     * Compute the reverse complement of the given sequence of bases.
     * <p>
     * NOTE: this method understands IUPAC codes.
     * <p>
     * NOTE 2: The complement of 'N' is returned as 'N'.
     * 
     * @param bases Sequence of bases to compute reverse complement for
     * @return  the given bases, reversed and converted to their DNA complement.
     */
    @CompileStatic
    static String reverseComplement(String bases) {
        byte [] bytes = bases.bytes
        StringBuilder result  = new StringBuilder()
        for(int i=bytes.length-1; i>=0; --i) {
            switch(bytes[i]) {
                case A:
                    result.append((char)T)
                    break
                case T:
                    result.append((char)A)
                    break
                case C:
                    result.append((char)G)
                    break
                case G:
                    result.append((char)C)
                    break
                case Y:
                    result.append((char)R)
                    break
                case R:
                    result.append((char)Y)
                    break
                case W:
                    result.append((char)W)
                    break
                case M:
                    result.append((char)K)
                    break
                case K:
                    result.append((char)M)
                    break
                case S:
                    result.append((char)S)
                    break
                case H:
                    result.append((char)D)
                    break
                case D:
                    result.append((char)H)
                    break
                case B:
                    result.append((char)V)
                    break
                case V:
                    result.append((char)B)
                    break
                case N:
                    result.append((char)N)
                    break
            }
        }
        return result.toString()
    }
    
    /**
     * Iterates over the FASTA and calls the given closure for each
     * reference sequence (eg: chromosome) passing the bases of the sequence
     * as a byte array
     */
    @CompileStatic
    void eachSequence(Closure c) {
        ProgressCounter.withProgress { ProgressCounter progress ->
            ReferenceSequence seq
            while((seq = this.indexedFastaFile.nextSequence()) != null) {
                progress.count()
                c(seq.name, seq.bases)
            }
        }
    }
    
    /**
     * Iterates over the ranges in the BED file and extracts the bases for the ranges,
     * passing the bases of the sequence
     * as a byte array to the given closure for each range.
     */
    @CompileStatic
    void eachSequence(Regions regions, Closure c) {
        boolean includeRegion = false
//        if(c.maximumNumberOfParameters > 2) 
//            includeRegion = true
        regions.eachRange(unique:true) {  String chr, int start, int end ->
            c(chr+":"+start+"-"+end, this.baseBytesAt(chr,start,end))
        }
    }
    
    @CompileStatic
    static String format(String contig, String sequence) {
        StringBuilder output = new StringBuilder()
        final int iterations = (int)Math.ceil(sequence.size()/80)
        for(int i=0; i<iterations; ++i) {
            output.append(sequence.substring(i*80, Math.min((i+1)*80,sequence.size())))
            output.append("\n")
        }
        return output.toString()
    }
    
    /**
     * Display given sequence with a single base bracketed to highlight it
     * 
     * @param seq   sequence to display
     * @param start position to start from (inclusive)
     * @return  sequence with specified base bracketed
     */    
    static String bracket(String seq, int position) {
        bracket(seq,position,position+1)
    }
    
    /**
     * Display given sequence with given bases bracketed
     * 
     * @param seq   sequence to display
     * @param start position to start from (inclusive)
     * @param end   position to end at (exclusive)
     * @return  sequence with specified bases bracketed
     */
    static String bracket(String seq, int start, int end) {
        "${seq.substring(0,start)}[${seq.substring(start,end)}]${seq.substring(end)}"
    }
}
