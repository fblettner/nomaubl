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
         Normalizes amounts and validates tax rate
         BG-23: VAT breakdown -->
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
            <!-- BT-116: VAT category taxable amount -->
            <cbc:TaxableAmount currencyID="{$currencyID}">
                <xsl:value-of select="ubl:normalize-amount($taxableAmount)" />
            </cbc:TaxableAmount>
            <!-- BT-117: VAT category tax amount -->
            <cbc:TaxAmount currencyID="{$currencyID}">
                <xsl:value-of select="ubl:normalize-amount($taxAmount)" />
            </cbc:TaxAmount>
            <cac:TaxCategory>
                <!-- BT-118: VAT category code -->
                <cbc:ID>
                    <xsl:value-of select="$taxID" />
                </cbc:ID>
                <xsl:variable name="validatedRate"
                    select="ubl:validate-tax-rate($taxRate, $invalidRateMarker)" />
                <xsl:if test="ubl:is-not-empty($validatedRate)">
                    <!-- BT-119: VAT category rate -->
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
         Normalizes amounts and validates tax rate
         BG-22: Document totals -->
    <xsl:template name="ubl:legal-monetary-total">
        <xsl:param name="totalWithoutVAT" as="xs:string?" />
        <xsl:param name="totalWithVAT"
            as="xs:string?" />
        <xsl:param name="currencyID" as="xs:string" />

        <cac:LegalMonetaryTotal>
            <!-- BT-106: Sum of Invoice line net amount -->
            <cbc:LineExtensionAmount currencyID="{$currencyID}">
                <xsl:value-of select="ubl:normalize-amount($totalWithoutVAT)" />
            </cbc:LineExtensionAmount>
            <!-- BT-109: Invoice total amount without VAT -->
            <cbc:TaxExclusiveAmount currencyID="{$currencyID}">
                <xsl:value-of
                    select="ubl:normalize-amount($totalWithoutVAT)" />
            </cbc:TaxExclusiveAmount>
            <!-- BT-112: Invoice total amount with VAT -->
            <cbc:TaxInclusiveAmount currencyID="{$currencyID}">
                <xsl:value-of
                    select="ubl:normalize-amount($totalWithVAT)" />
            </cbc:TaxInclusiveAmount>
            <!-- BT-115: Amount due for payment -->
            <cbc:PayableAmount currencyID="{$currencyID}">
                <xsl:value-of
                    select="ubl:normalize-amount($totalWithVAT)" />
            </cbc:PayableAmount>
        </cac:LegalMonetaryTotal>
    </xsl:template>

    <!-- Create Contract ID if exists
         BT-12: Contract reference -->
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

    <!-- Create PartyName if exists
         BT-28: Seller trading name -->
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
         Only creates address block if at least one field is non-empty
         BG-5: Seller postal address / BG-8: Buyer postal address / BG-15: Deliver to address -->
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
                <!-- BT-35/50/75: Address line 1 -->
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:StreetName'" />
                    <xsl:with-param name="value" select="$street" />
                </xsl:call-template>

                <!-- Additional street: concatenate street2, street3, street4 with space separator -->
                <!-- BT-36/51/76: Address line 2 -->
                <xsl:variable
                    name="additionalStreet"
                    select="normalize-space(concat(normalize-space($street2), ' ', normalize-space($street3), ' ', normalize-space($street4)))" />
                <xsl:call-template
                    name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:AdditionalStreetName'" />
                    <xsl:with-param name="value" select="$additionalStreet" />
                </xsl:call-template>

                <!-- BT-37/52/77: City -->
                <xsl:call-template
                    name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:CityName'" />
                    <xsl:with-param name="value" select="$city" />
                </xsl:call-template>

                <!-- BT-38/53/78: Post code -->
                <xsl:call-template
                    name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:PostalZone'" />
                    <xsl:with-param name="value" select="$postalCode" />
                </xsl:call-template>

                <!-- BT-40/55/80: Country code -->
                <xsl:call-template
                    name="ubl:country">
                    <xsl:with-param name="code" select="$countryCode" />
                    <xsl:with-param name="default" select="$defaultCountry" />
                </xsl:call-template>
            </xsl:element>

    </xsl:template>

    <!-- Create Contact
         BG-6: Seller contact -->
    <xsl:template name="ubl:contact">
        <xsl:param name="name" as="xs:string?" />
        <xsl:param name="telephone" as="xs:string?" />
        <xsl:param
            name="electronicMail" as="xs:string?" />
        
                  <xsl:if
            test="ubl:is-not-empty($name) or ubl:is-not-empty($telephone) or ubl:is-not-empty($electronicMail)">
            <cac:Contact>
                <!-- BT-41: Seller contact point -->
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:Name'" />
                    <xsl:with-param name="value" select="$name" />
                </xsl:call-template>
                <!-- BT-42: Seller contact telephone number -->
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:Telephone'" />
                    <xsl:with-param name="value" select="$telephone" />
                </xsl:call-template>
                <!-- BT-43: Seller contact email address -->
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:ElectronicMail'" />
                    <xsl:with-param name="value" select="$electronicMail" />
                </xsl:call-template>
            </cac:Contact>
        </xsl:if>
    </xsl:template>

    <!-- Create PartyLegalEntity
         BT-44: Buyer name / BT-27: Seller name -->
    <xsl:template name="ubl:party-legal-entity">
        <xsl:param name="customerName" as="xs:string?" />
        <xsl:param name="customerSiren"
            as="xs:string?" />
        
        <xsl:if
            test="ubl:is-not-empty($customerName) or ubl:is-not-empty($customerSiren)">
            <cac:PartyLegalEntity>
                <!-- BT-44: Buyer name / BT-27: Seller name -->
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:RegistrationName'" />
                    <xsl:with-param name="value" select="$customerName" />
                </xsl:call-template>

                <!-- BT-47: Buyer legal registration identifier / BT-30: Seller legal registration identifier -->
                <xsl:if test="ubl:is-not-empty($customerSiren)">
                    <xsl:call-template name="ubl:company-siren">
                        <xsl:with-param name="siren" select="$customerSiren" />
                        <xsl:with-param name="missingMarker" select="''" />
                    </xsl:call-template>
                </xsl:if>
            </cac:PartyLegalEntity>
        </xsl:if>
    </xsl:template>

    <!-- Create PaymentMeans
         BG-16: Payment instructions -->
    <xsl:template name="ubl:payment-means">
        <xsl:param name="paymentCode" as="xs:string?" />
        <xsl:param name="iban" as="xs:string?" />
        <xsl:param
            name="swift" as="xs:string?" />
        <xsl:param name="supplierName" as="xs:string?" />
        
        <xsl:if
            test="ubl:is-not-empty($paymentCode) or ubl:is-not-empty($iban) or ubl:is-not-empty($swift) or ubl:is-not-empty($supplierName)">
            <cac:PaymentMeans>
                <!-- BT-81: Payment means type code -->
                <xsl:if test="ubl:is-not-empty($paymentCode)">
                    <cbc:PaymentMeansCode>
                        <xsl:value-of select="$paymentCode" />
                    </cbc:PaymentMeansCode>
                </xsl:if>

                <xsl:if
                    test="ubl:is-not-empty($iban) or ubl:is-not-empty($swift) or ubl:is-not-empty($supplierName)">
                    <cac:PayeeFinancialAccount>
                        <!-- BT-84: Payment account identifier (IBAN) -->
                        <xsl:call-template name="ubl:emit">
                            <xsl:with-param name="qname" select="'cbc:ID'" />
                            <xsl:with-param name="value" select="$iban" />
                        </xsl:call-template>

                        <!-- BT-85: Payment account name -->
                        <xsl:call-template name="ubl:emit">
                            <xsl:with-param name="qname" select="'cbc:Name'" />
                            <xsl:with-param name="value" select="$supplierName" />
                        </xsl:call-template>

                        <!-- BT-86: Payment service provider identifier (BIC) -->
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

    <!-- Create ClassifiedTaxCategory
         BT-151: Invoiced item VAT category code
         BT-152: Invoiced item VAT rate -->
    <xsl:template name="ubl:classified-tax-category">
        <xsl:param name="id" as="xs:string?" />
        <xsl:param name="percent" as="xs:string?" />
        
        <cac:ClassifiedTaxCategory>
            <!-- BT-151: Invoiced item VAT category code -->
            <xsl:call-template name="ubl:emit">
                <xsl:with-param name="qname" select="'cbc:ID'" />
                <xsl:with-param name="value" select="$id" />
            </xsl:call-template>
            <!-- BT-152: Invoiced item VAT rate -->
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

    <!-- Create AdditionalItemProperty
         BG-32: Item attributes -->
    <xsl:template name="ubl:additional-item-property">
        <xsl:param name="name" as="xs:string?" />
        <xsl:param name="value" as="xs:string?" />
        
        <xsl:if
            test="ubl:is-not-empty($value)">
            <cac:AdditionalItemProperty>
                <!-- BT-160: Item attribute name -->
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:Name'" />
                    <xsl:with-param name="value" select="$name" />
                </xsl:call-template>
                <!-- BT-161: Item attribute value -->
                <xsl:call-template name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:Value'" />
                    <xsl:with-param name="value" select="$value" />
                </xsl:call-template>
            </cac:AdditionalItemProperty>
        </xsl:if>
    </xsl:template>

    <!-- Create Price
         BG-29: Price details -->
    <xsl:template name="ubl:line-price">
        <xsl:param name="unitPrice" as="xs:string?" />
        <xsl:param name="unitCode" as="xs:string?" />
        <xsl:param name="currencyCode" as="xs:string?" />
        <xsl:param name="discountAmount" as="xs:string?" />

        
        <cac:Price>
            <!-- BT-146: Item net price -->
            <xsl:call-template name="ubl:emit-scheme">
                <xsl:with-param name="qname" select="'cbc:PriceAmount'" />
                <xsl:with-param name="value" select="ubl:normalize-amount($unitPrice)" />
                <xsl:with-param name="code" select="'currencyID'" />
                <xsl:with-param name="codeValue" select="$currencyCode" />
            </xsl:call-template>

            <!-- BT-149: Item price base quantity -->
            <xsl:call-template name="ubl:emit-scheme">
                <xsl:with-param name="qname" select="'cbc:BaseQuantity'" />
                <xsl:with-param name="value" select="ubl:normalize-amount('1')" />
                <xsl:with-param name="code" select="'unitCode'" />
                <xsl:with-param name="codeValue" select="$unitCode" />
            </xsl:call-template>

            <!-- AllowanceCharge only if remise > 0 -->
            <!-- BG-27: Line level allowances (if applicable) -->
            <xsl:if test="number($discountAmount) &gt; 0">
                <cac:AllowanceCharge>
                    <cbc:ChargeIndicator>false</cbc:ChargeIndicator>

                    <!-- BT-147: Item price discount (amount) -->
                    <xsl:call-template name="ubl:emit-scheme">
                        <xsl:with-param name="qname" select="'cbc:Amount'" />
                        <xsl:with-param name="value" select="ubl:normalize-amount($discountAmount)" />
                        <xsl:with-param name="code" select="'currencyID'" />
                        <xsl:with-param name="codeValue" select="$currencyCode" />
                    </xsl:call-template>

                    <!-- BT-148: Item gross price -->
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