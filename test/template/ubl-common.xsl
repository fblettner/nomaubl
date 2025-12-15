<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:ubl="urn:nomana:ubl:common"
    xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
    xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
    exclude-result-prefixes="xs ubl">

    <!-- =========================
         Shared UBL Utility Functions
         ========================= -->

    <!-- Emit element with text only if non-empty -->
    <xsl:template name="ubl:emit">
        <xsl:param name="qname" as="xs:string" />
        <xsl:param name="value" as="xs:string?" />
        <xsl:if
            test="ubl:is-not-empty($value)">
            <xsl:element name="{$qname}">
                <xsl:value-of select="$value" />
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!-- Emit element with text + optional schemeID only if value non-empty -->
    <xsl:template name="ubl:emit-scheme">
        <xsl:param name="qname" as="xs:string" />
        <xsl:param name="value" as="xs:string?" />
        <xsl:param
            name="code" as="xs:string?" />
        <xsl:param name="codeValue" as="xs:string?" />
        <xsl:if
            test="ubl:is-not-empty($value)">
            <xsl:element name="{$qname}">
                <xsl:if test="ubl:is-not-empty($codeValue)">
                    <xsl:attribute name="{$code}" select="$codeValue" />
                </xsl:if>
                <xsl:value-of
                    select="$value" />
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!-- Normalize French amount format to decimal
         ex: "1 160,55 €" -> "1160.55" -->
    <xsl:function name="ubl:normalize-amount" as="xs:string">
        <xsl:param name="value" as="xs:string?" />
        
        <xsl:variable name="trim" select="normalize-space($value)" />
        
        <!-- Clean up using nested translate() to remove spaces, €, and non-breaking spaces -->
        <xsl:variable name="clean"
            select="translate(
                       translate(
                         translate(
                           translate($trim,
                                     concat(' ', '€', '&#160;', '&#x202F;'),
                                     ''
                           ),
                           ',', '.'
                         ),
                         '&#160;', ''
                       ),
                       '&#x202F;', ''
                   )" />
        
        <xsl:choose>
            <!-- empty value -->
            <xsl:when test="$clean = ''">0.00</xsl:when>

            <!-- starts with .XXX -->
            <xsl:when test="starts-with($clean, '.')">
                <xsl:value-of select="concat('0', $clean)" />
            </xsl:when>

            <!-- contains decimal point -->
            <xsl:when test="contains($clean, '.')">
                <xsl:variable name="intPart" select="substring-before($clean, '.')" />
                <xsl:variable name="decPart" select="substring-after($clean, '.')" />
                <xsl:value-of select="concat($intPart, '.', substring($decPart, 1, 2))" />
            </xsl:when>

            <!-- integer -->
            <xsl:otherwise>
                <xsl:value-of select="$clean" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Normalize percentage rate
         ex: "20,00%" -> "20.00" -->
    <xsl:function name="ubl:normalize-percent" as="xs:string">
        <xsl:param name="value" as="xs:string?" />
        
        <xsl:variable name="trim"
            select="normalize-space($value)" />
        <xsl:variable name="noPercent"
            select="replace($trim, '%', '')" />
        <xsl:variable name="normalized"
            select="replace($noPercent, ',', '.')" />
        
        <xsl:choose>
            <!-- empty value -->
            <xsl:when test="$normalized = ''">
                <xsl:text></xsl:text>
            </xsl:when>

            <!-- contains decimal point -->
            <xsl:when test="contains($normalized, '.')">
                <xsl:variable name="intPart" select="substring-before($normalized, '.')" />
                <xsl:variable
                    name="decPart" select="substring-after($normalized, '.')" />
                <xsl:value-of
                    select="concat($intPart, '.', substring($decPart, 1, 2))" />
            </xsl:when>

            <!-- integer -->
            <xsl:otherwise>
                <xsl:value-of select="$normalized" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Extract tax rate from descriptive text
         ex: "montant base taxable 20,00 % sur" -> "20.00" -->
    <xsl:function name="ubl:extract-tax-rate" as="xs:string">
        <xsl:param name="text" as="xs:string?" />
        
        <xsl:variable name="trim" select="normalize-space($text)" />
        
        <xsl:choose>
            <!-- empty value -->
            <xsl:when test="$trim = ''">
                <xsl:text></xsl:text>
            </xsl:when>
            
            <!-- contains pattern like "20,00 %" or "20.00%" -->
            <xsl:when test="matches($trim, '\d+[,\.]\d+\s*%')">
                <xsl:variable name="extracted" 
                    select="replace($trim, '.*?(\d+[,\.]\d+)\s*%.*', '$1')" />
                <xsl:value-of select="ubl:normalize-percent($extracted)" />
            </xsl:when>
            
            <!-- contains pattern like "20 %" (integer) -->
            <xsl:when test="matches($trim, '\d+\s*%')">
                <xsl:variable name="extracted" 
                    select="replace($trim, '.*?(\d+)\s*%.*', '$1')" />
                <xsl:value-of select="$extracted" />
            </xsl:when>
            
            <!-- fallback: try to normalize as-is -->
            <xsl:otherwise>
                <xsl:value-of select="ubl:normalize-percent($trim)" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Normalize quantity with up to 4 decimal places -->
    <xsl:function name="ubl:normalize-quantity" as="xs:string">
        <xsl:param name="value" as="xs:string?" />
        
        <xsl:variable name="trim"
            select="normalize-space($value)" />
          
        <!-- Clean up using nested translate() to remove spaces, €, and non-breaking spaces -->
        <xsl:variable name="clean"
            select="translate(
                       translate(
                         translate(
                           translate($trim,
                                     concat(' ', '€', '&#160;', '&#x202F;'),
                                     ''
                           ),
                           ',', '.'
                         ),
                         '&#160;', ''
                       ),
                       '&#x202F;', ''
                   )" />

        <xsl:choose>
            <!-- empty value -->
            <xsl:when test="$clean = ''">**INVALID_BT-129_QUANTITY**</xsl:when>

            <!-- starts with -.XXX -->
            <xsl:when test="starts-with($clean, '-.')">
                <xsl:value-of select="concat('-0', substring($clean, 2))" />
            </xsl:when>

            <!-- starts with .XXX -->
            <xsl:when test="starts-with($clean, '.')">
                <xsl:value-of select="concat('0', $clean)" />
            </xsl:when>

            <!-- contains decimal point -->
            <xsl:when test="contains($clean, '.')">
                <xsl:variable name="intPart" select="substring-before($clean, '.')" />
                <xsl:variable
                    name="decPart" select="substring-after($clean, '.')" />
                <xsl:value-of
                    select="concat($intPart, '.', substring($decPart, 1, 4))" />
            </xsl:when>

            <!-- integer -->
            <xsl:otherwise>
                <xsl:value-of select="$clean" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Convert unit of measure to UBL code -->
    <xsl:function name="ubl:unit-code" as="xs:string">
        <xsl:param name="um" as="xs:string?" />
        <xsl:choose>
            <xsl:when test="$um = ('T', 'TONNE')">TNE</xsl:when>
            <xsl:when test="$um = 'L'">LTR</xsl:when>
            <xsl:when test="$um = 'KG'">KGM</xsl:when>
            <xsl:when test="$um = 'UN'">C62</xsl:when>
            <xsl:when test="ubl:is-not-empty($um)">
                <xsl:value-of select="$um" />
            </xsl:when>
            <xsl:otherwise>C62</xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Check if string is non-empty after normalization -->
    <xsl:function name="ubl:is-not-empty" as="xs:boolean">
        <xsl:param name="value" as="xs:string?" />
        <xsl:sequence
            select="normalize-space($value) != ''" />
    </xsl:function>

    <!-- Validate and normalize tax rate with error marker if invalid -->
    <xsl:function name="ubl:validate-tax-rate" as="xs:string">
        <xsl:param name="value" as="xs:string?" />
        <xsl:param name="invalidMarker" as="xs:string" />
        
        <xsl:variable
            name="normalized" select="normalize-space($value)" />
        
        <xsl:choose>
            <!-- empty -->
            <xsl:when test="$normalized = ''">
                <xsl:value-of select="$invalidMarker" />
            </xsl:when>

            <!-- not a number -->
            <xsl:when test="not(number($normalized) = number($normalized))">
                <xsl:value-of select="$invalidMarker" />
            </xsl:when>

            <!-- valid number -->
            <xsl:otherwise>
                <xsl:value-of select="$normalized" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Create TaxSubtotal with validation
         Normalizes amounts and validates tax rate -->
    <xsl:template name="ubl:tax-subtotal">
        <xsl:param name="taxableAmount" as="xs:string?" />
        <xsl:param name="taxAmount"
            as="xs:string?" />
        <xsl:param name="taxRate" as="xs:string?" />
        <xsl:param name="taxID"
            as="xs:string?" />
        <xsl:param name="currencyID" as="xs:string" />
        <xsl:param
            name="invalidRateMarker" as="xs:string" select="'**INVALID_TAX_RATE**'" />
        
        <cac:TaxSubtotal>
            <cbc:TaxableAmount currencyID="{$currencyID}">
                <xsl:value-of select="ubl:normalize-amount($taxableAmount)" />
            </cbc:TaxableAmount>
            <cbc:TaxAmount currencyID="{$currencyID}">
                <xsl:value-of select="ubl:normalize-amount($taxAmount)" />
            </cbc:TaxAmount>
            <cac:TaxCategory>
                <cbc:ID>
                    <xsl:value-of select="$taxID" />
                </cbc:ID>
                <xsl:variable name="validatedRate"
                    select="ubl:validate-tax-rate($taxRate, $invalidRateMarker)" />
                <xsl:if test="ubl:is-not-empty($validatedRate)">
                    <cbc:Percent>
                        <xsl:value-of select="$validatedRate" />
                    </cbc:Percent>
                </xsl:if>
                <cac:TaxScheme>
                    <cbc:ID>VAT</cbc:ID>
                </cac:TaxScheme>
            </cac:TaxCategory>
        </cac:TaxSubtotal>
    </xsl:template>

    <!-- Create LegalMonetaryTotal with validation
         Normalizes amounts and validates tax rate -->
    <xsl:template name="ubl:legal-monetary-total">
        <xsl:param name="totalWithoutVAT" as="xs:string?" />
        <xsl:param name="totalWithVAT"
            as="xs:string?" />
        <xsl:param name="currencyID" as="xs:string" />

        <!-- Create  with validation
         Normalizes amounts and validates tax rate -->
        <cac:LegalMonetaryTotal>
            <cbc:LineExtensionAmount currencyID="{$currencyID}">
                <xsl:value-of select="ubl:normalize-amount($totalWithoutVAT)" />
            </cbc:LineExtensionAmount>
            <cbc:TaxExclusiveAmount currencyID="{$currencyID}">
                <xsl:value-of
                    select="ubl:normalize-amount($totalWithoutVAT)" />
            </cbc:TaxExclusiveAmount>
            <cbc:TaxInclusiveAmount currencyID="{$currencyID}">
                <xsl:value-of
                    select="ubl:normalize-amount($totalWithVAT)" />
            </cbc:TaxInclusiveAmount>
            <cbc:PayableAmount currencyID="{$currencyID}">
                <xsl:value-of
                    select="ubl:normalize-amount($totalWithVAT)" />
            </cbc:PayableAmount>
        </cac:LegalMonetaryTotal>
    </xsl:template>

    <!-- Create Contract ID if exists -->
    <xsl:template name="ubl:contract-id">
        <xsl:param name="contractId" as="xs:string?" />

        <xsl:if test="ubl:is-not-empty($contractId)">
            <cac:ContractDocumentReference>
                <cbc:ID>
                    <xsl:value-of select="$contractId" />
                </cbc:ID>
            </cac:ContractDocumentReference>
        </xsl:if>
    </xsl:template>

    <!-- Create PartyName if exists -->
    <xsl:template name="ubl:party-name">
        <xsl:param name="partyName" as="xs:string?" />

        <xsl:if test="ubl:is-not-empty($partyName)">
            <cac:PartyName>
                <cbc:Name>
                    <xsl:value-of select="$partyName" />
                </cbc:Name>
            </cac:PartyName>
        </xsl:if>
    </xsl:template>

    <!-- Create PostalAddress with street, city, postal code, and country
         Only creates address block if at least one field is non-empty -->
    <xsl:template name="ubl:address">
        <xsl:param name="qname" as="xs:string" />
        <xsl:param name="street" as="xs:string?" />
        <xsl:param
            name="street2" as="xs:string?" />
        <xsl:param name="street3" as="xs:string?" />
        <xsl:param
            name="street4" as="xs:string?" />
        <xsl:param name="city" as="xs:string?" />
        <xsl:param
            name="postalCode" as="xs:string?" />
        <xsl:param name="countryCode" as="xs:string?" />
        <xsl:param
            name="defaultCountry" as="xs:string" />
        
            <xsl:element name="{$qname}">
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:StreetName'" />
                    <xsl:with-param name="value" select="$street" />
                </xsl:call-template>

                <!-- Additional street: concatenate street2, street3, street4 with space separator -->
                <xsl:variable
                    name="additionalStreet"
                    select="normalize-space(concat(normalize-space($street2), ' ', normalize-space($street3), ' ', normalize-space($street4)))" />
                <xsl:call-template
                    name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:AdditionalStreetName'" />
                    <xsl:with-param name="value" select="$additionalStreet" />
                </xsl:call-template>

                <xsl:call-template
                    name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:CityName'" />
                    <xsl:with-param name="value" select="$city" />
                </xsl:call-template>

                <xsl:call-template
                    name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:PostalZone'" />
                    <xsl:with-param name="value" select="$postalCode" />
                </xsl:call-template>

                <xsl:call-template
                    name="ubl:country">
                    <xsl:with-param name="code" select="$countryCode" />
                    <xsl:with-param name="default" select="$defaultCountry" />
                </xsl:call-template>
            </xsl:element>

    </xsl:template>

    <!-- Create Contact -->
    <xsl:template name="ubl:contact">
        <xsl:param name="name" as="xs:string?" />
        <xsl:param name="telephone" as="xs:string?" />
        <xsl:param
            name="electronicMail" as="xs:string?" />
        
                  <xsl:if
            test="ubl:is-not-empty($name) or ubl:is-not-empty($telephone) or ubl:is-not-empty($electronicMail)">
            <cac:Contact>
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:Name'" />
                    <xsl:with-param name="value" select="$name" />
                </xsl:call-template>
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:Telephone'" />
                    <xsl:with-param name="value" select="$telephone" />
                </xsl:call-template>
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:ElectronicMail'" />
                    <xsl:with-param name="value" select="$electronicMail" />
                </xsl:call-template>
            </cac:Contact>
        </xsl:if>
    </xsl:template>

    <!-- Create PartyLegalEntity -->
    <xsl:template name="ubl:party-legal-entity">
        <xsl:param name="customerName" as="xs:string?" />
        <xsl:param name="customerSiren"
            as="xs:string?" />
        
        <xsl:if
            test="ubl:is-not-empty($customerName) or ubl:is-not-empty($customerSiren)">
            <cac:PartyLegalEntity>
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:RegistrationName'" />
                    <xsl:with-param name="value" select="$customerName" />
                </xsl:call-template>

                <xsl:if test="ubl:is-not-empty($customerSiren)">
                    <xsl:call-template name="ubl:company-siren">
                        <xsl:with-param name="siren" select="$customerSiren" />
                        <xsl:with-param name="missingMarker" select="''" />
                    </xsl:call-template>
                </xsl:if>
            </cac:PartyLegalEntity>
        </xsl:if>
    </xsl:template>

    <!-- Create PaymentMeans -->
    <xsl:template name="ubl:payment-means">
        <xsl:param name="paymentCode" as="xs:string?" />
        <xsl:param name="iban" as="xs:string?" />
        <xsl:param
            name="swift" as="xs:string?" />
        <xsl:param name="supplierName" as="xs:string?" />
        
        <xsl:if
            test="ubl:is-not-empty($paymentCode) or ubl:is-not-empty($iban) or ubl:is-not-empty($swift) or ubl:is-not-empty($supplierName)">
            <cac:PaymentMeans>
                <xsl:if test="ubl:is-not-empty($paymentCode)">
                    <cbc:PaymentMeansCode>
                        <xsl:value-of select="$paymentCode" />
                    </cbc:PaymentMeansCode>
                </xsl:if>

                <xsl:if
                    test="ubl:is-not-empty($iban) or ubl:is-not-empty($swift) or ubl:is-not-empty($supplierName)">
                    <cac:PayeeFinancialAccount>
                        <xsl:call-template name="ubl:emit">
                            <xsl:with-param name="qname" select="'cbc:ID'" />
                            <xsl:with-param name="value" select="$iban" />
                        </xsl:call-template>

                        <xsl:call-template name="ubl:emit">
                            <xsl:with-param name="qname" select="'cbc:Name'" />
                            <xsl:with-param name="value" select="$supplierName" />
                        </xsl:call-template>

                        <xsl:if test="ubl:is-not-empty($swift)">
                            <cac:FinancialInstitutionBranch>
                                <cbc:ID>
                                    <xsl:value-of select="$swift" />
                                </cbc:ID>
                            </cac:FinancialInstitutionBranch>
                        </xsl:if>

                    </cac:PayeeFinancialAccount>
                </xsl:if>
            </cac:PaymentMeans>
        </xsl:if>
    </xsl:template>

    <!-- Create ClassifiedTaxCategory -->
    <xsl:template name="ubl:classified-tax-category">
        <xsl:param name="id" as="xs:string?" />
        <xsl:param name="percent" as="xs:string?" />
        
        <cac:ClassifiedTaxCategory>
            <xsl:call-template name="ubl:emit">
                <xsl:with-param name="qname" select="'cbc:ID'" />
                <xsl:with-param name="value" select="$id" />
            </xsl:call-template>
            <xsl:call-template name="ubl:emit">
                <xsl:with-param name="qname" select="'cbc:Percent'" />
                <xsl:with-param name="value" select="$percent" />
            </xsl:call-template>
            <cac:TaxScheme>
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:ID'" />
                    <xsl:with-param name="value" select="'VAT'" />
                </xsl:call-template>
            </cac:TaxScheme>
        </cac:ClassifiedTaxCategory>
    </xsl:template>

    <!-- Create AdditionalItemProperty -->
    <xsl:template name="ubl:additional-item-property">
        <xsl:param name="name" as="xs:string?" />
        <xsl:param name="value" as="xs:string?" />
        
        <xsl:if
            test="ubl:is-not-empty($value)">
            <cac:AdditionalItemProperty>
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:Name'" />
                    <xsl:with-param name="value" select="$name" />
                </xsl:call-template>
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:Value'" />
                    <xsl:with-param name="value" select="$value" />
                </xsl:call-template>
            </cac:AdditionalItemProperty>
        </xsl:if>
    </xsl:template>

    <!-- Create Price -->
    <xsl:template name="ubl:line-price">
        <xsl:param name="unitPrice" as="xs:string?" />
        <xsl:param name="unitCode" as="xs:string?" />
        <xsl:param name="currencyCode" as="xs:string?" />
        <xsl:param name="discountAmount" as="xs:string?" />

        
        <cac:Price>
            <!-- PriceAmount: only if non-empty -->
            <xsl:call-template name="ubl:emit-scheme">
                <xsl:with-param name="qname" select="'cbc:PriceAmount'" />
                <xsl:with-param name="value" select="ubl:normalize-amount($unitPrice)" />
                <xsl:with-param name="code" select="'currencyID'" />
                <xsl:with-param name="codeValue" select="$currencyCode" />
            </xsl:call-template>

            <xsl:call-template name="ubl:emit-scheme">
                <xsl:with-param name="qname" select="'cbc:BaseQuantity'" />
                <xsl:with-param name="value" select="ubl:normalize-amount('1')" />
                <xsl:with-param name="code" select="'unitCode'" />
                <xsl:with-param name="codeValue" select="$unitCode" />
            </xsl:call-template>

            <!-- AllowanceCharge only if remise > 0 -->
            <xsl:if test="number($discountAmount) &gt; 0">
                <cac:AllowanceCharge>
                    <cbc:ChargeIndicator>false</cbc:ChargeIndicator>

                    <xsl:call-template name="ubl:emit-scheme">
                        <xsl:with-param name="qname" select="'cbc:Amount'" />
                        <xsl:with-param name="value" select="ubl:normalize-amount($discountAmount)" />
                        <xsl:with-param name="code" select="'currencyID'" />
                        <xsl:with-param name="codeValue" select="$currencyCode" />
                    </xsl:call-template>

                    <xsl:call-template name="ubl:emit-scheme">
                        <xsl:with-param name="qname" select="'cbc:BaseAmount'" />
                        <xsl:with-param name="value" select="ubl:normalize-amount(string(xs:decimal($unitPrice) + xs:decimal($discountAmount)))" />
                        <xsl:with-param name="code" select="'currencyID'" />
                        <xsl:with-param name="codeValue" select="$currencyCode" />
                    </xsl:call-template>

                </cac:AllowanceCharge>
            </xsl:if>
        </cac:Price>
    </xsl:template>

    <!-- Utilitaire : extraire code postal (5 premiers chiffres) -->
    <xsl:function name="ubl:postal-code" as="xs:string">
        <xsl:param name="value" />
        <xsl:value-of
            select="substring(normalize-space($value), 1, 5)" />
    </xsl:function>

    <!-- Utilitaire : extraire ville (après les 5 premiers caractères) -->
    <xsl:function name="ubl:city-name" as="xs:string">
        <xsl:param name="value" />
        <xsl:value-of
            select="normalize-space(substring(normalize-space($value), 6))" />
    </xsl:function>

</xsl:stylesheet>