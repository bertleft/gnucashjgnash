/*
 * Copyright 2017 Albert Santos.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package gnucashjgnash.imports;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents a parsed GnuCash GncNumeric from <a href="https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc" target="_blank" rel="noopener noreferrer">gnucash-v2.rnc</a>
 * @author albert
 *
 */
public class NumericEntry extends ParsedEntry {
	BigInteger numerator = null;
    BigInteger denominator = null;
    int scale;
    
    /**
	 * @param parentParsedEntry
	 */
	protected NumericEntry(ParsedEntry parentParsedEntry) {
		super(null);
		this.parentSource = parentParsedEntry;
	}

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.numerator);
        hash = 83 * hash + Objects.hashCode(this.denominator);
        hash = 83 * hash + this.scale;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NumericEntry other = (NumericEntry) obj;
        if (this.scale != other.scale) {
            return false;
        }
        if (!Objects.equals(this.numerator, other.numerator)) {
            return false;
        }
        if (!Objects.equals(this.denominator, other.denominator)) {
            return false;
        }
        return true;
    }

    
    /* (non-Javadoc)
	 * @see gnucashjgnash.imports.ParsedEntry#getIndentifyingText(gnucashjgnash.imports.GnuCashToJGnashContentHandler)
	 */
	@Override
	public String getIndentifyingText(GnuCashToJGnashContentHandler contentHandler) {
		return null;
	}




	public void fromRealString(String valueText, BigInteger denominator) {
    	float value = Float.parseFloat(valueText) * denominator.floatValue();
    	this.numerator = BigInteger.valueOf(Math.round(value));
    	this.denominator = denominator;
    }
    

    public static class NumericStateHandler extends GnuCashToJGnashContentHandler.AbstractStateHandler {
        final NumericEntry numericEntry;
        NumericStateHandler(final NumericEntry numericEntry, GnuCashToJGnashContentHandler contentHandler,
                          GnuCashToJGnashContentHandler.StateHandler parentStateHandler, String elementName) {
            super(contentHandler, parentStateHandler, elementName);
            this.numericEntry = numericEntry;
            this.numericEntry.updateLocatorInfo(contentHandler);
        }

        
		/* (non-Javadoc)
		 * @see gnucashjgnash.imports.GnuCashToJGnashContentHandler.StateHandler#getParsedEntry()
		 */
        @Override
        public ParsedEntry getParsedEntry() {
        	return this.numericEntry;
        }

        @Override
        protected void endState() {
            super.endState();

            this.numericEntry.numerator = null;
            this.numericEntry.denominator = null;

            int dividerIndex = this.characters.indexOf('/');
            if (dividerIndex < 0) {
                this.contentHandler.recordWarning(this.numericEntry.parentSource, "Message.Parse.XMLNumericDividerMissing", this.elementName);
                return;
            }

            String numeratorText = this.characters.substring(0, dividerIndex);
            String denominatorText = this.characters.substring(dividerIndex + 1);

            try {
                this.numericEntry.numerator = new BigInteger(numeratorText);
                this.numericEntry.denominator = new BigInteger(denominatorText);
                this.numericEntry.scale = (int)Math.round(Math.log10(this.numericEntry.denominator.intValue()));
            }
            catch (NumberFormatException e) {
            	this.contentHandler.recordWarning(this.numericEntry.parentSource, "Message.Parse.XMLNumericValueInvalid", this.elementName);
            }
        }
    }

    boolean validateParse(GnuCashToJGnashContentHandler.StateHandler stateHandler, String qName) {
        return (this.numerator != null) && (this.denominator != null);
    }


    public BigDecimal toBigDecimal() {
        return new BigDecimal(this.numerator).divide(new BigDecimal(this.denominator));
    }
    
    public BigDecimal divide(NumericEntry divisor) {
    	int scale = this.scale + divisor.scale;
        BigDecimal bdNumerator = new BigDecimal(this.numerator).multiply(new BigDecimal(divisor.denominator)).setScale(scale);
        BigDecimal bdDenominator = new BigDecimal(this.denominator).multiply(new BigDecimal(divisor.numerator)).setScale(scale);
        return bdNumerator.divide(bdDenominator, RoundingMode.HALF_EVEN);
    }
}
