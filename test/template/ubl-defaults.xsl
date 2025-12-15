<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:ubl="urn:nomana:ubl:common"
    xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
    xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
    exclude-result-prefixes="xs ubl">

    <!-- =========================
         UBL Default Values and Business Rules
         ========================= -->
    <xsl:variable name="defaultCurrency" select="'EUR'" />
    <!-- =========================
         UBL Invoice Header Constants
         ========================= -->
    
         
    <!-- UBL Standard Header Elements
         Creates UBLVersionID, CustomizationID, and ProfileID with standard values -->
    <xsl:template name="ubl:invoice-header">
        <xsl:param name="ublVersion" as="xs:string" select="'2.1'" />
        <xsl:param name="customizationID" as="xs:string" select="'urn:cen.eu:en16931:2017'" />
        <xsl:param name="profileID" as="xs:string" select="'M1'" />
        
        <cbc:UBLVersionID>
            <xsl:value-of select="$ublVersion" />
        </cbc:UBLVersionID>
        <cbc:CustomizationID>
            <xsl:value-of select="$customizationID" />
        </cbc:CustomizationID>
        <cbc:ProfileID>
            <xsl:value-of select="$profileID" />
        </cbc:ProfileID>
    </xsl:template>

    <!-- =========================
         Payment Code Mapping
         ========================= -->
    
    <!-- Map payment codes to UBL payment means codes
         S -> 30 (Credit transfer)
         R -> 42 (Payment to bank account)
         Returns original code if no mapping found -->
    <xsl:function name="ubl:payment-code" as="xs:string">
        <xsl:param name="code" as="xs:string?" />
        
        <xsl:variable name="normalizedCode" select="normalize-space($code)" />
        
        <xsl:choose>
            <xsl:when test="$normalizedCode = 'S'">49</xsl:when>
            <xsl:when test="$normalizedCode = '4'">30</xsl:when>
             <xsl:otherwise>
                <xsl:value-of select="$normalizedCode" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- =========================
         Constant Notes for French Legal Requirements
         ========================= -->
    
    <!-- French Payment Delay Penalty Note (BR-FR) -->
    <xsl:template name="ubl:note-payment-delay">
        <cbc:Note>#PMD#Tout retard de paiement engendre une pénalité exigible à compter de la date d'échéance, calculée sur la base de trois fois le taux d'intérêt légal.</cbc:Note>
    </xsl:template>

    <!-- French Recovery Fee Note (BR-FR) -->
    <xsl:template name="ubl:note-recovery-fee">
        <cbc:Note>#PMT#Indemnité forfaitaire pour frais de recouvrement en cas de retard de paiement : 40 €.</cbc:Note>
    </xsl:template>

    <!-- French Legal Notes with optional AAB (General Terms) Note
         Includes payment delay penalty, recovery fee, and optional custom note -->
    <xsl:template name="ubl:notes-french-legal">
        <xsl:param name="AAB" as="xs:string?" />
        
        <xsl:call-template name="ubl:note-payment-delay" />
        <xsl:call-template name="ubl:note-recovery-fee" />
        
        <!-- AAB (General Terms) - only if non-empty -->
        <xsl:if test="ubl:is-not-empty($AAB)">
            <cbc:Note>
                <xsl:value-of select="concat('#AAB#', normalize-space($AAB))" />
            </cbc:Note>
        </xsl:if>
    </xsl:template>

    <!-- =========================
         Identity and Registration Templates
         ========================= -->

    <!-- Create PartyIdentification with SIREN (schemeID=0002)
         Returns default error marker if SIREN is empty -->
    <xsl:template name="ubl:party-siren">
        <xsl:param name="siren" as="xs:string?" />
        <xsl:param name="missingMarker" as="xs:string" select="'**MISSING_SIREN**'" />
        
        <cac:PartyIdentification>
            <cbc:ID schemeID="0002">
                <xsl:choose>
                    <xsl:when test="normalize-space($siren) != ''">
                        <xsl:value-of select="normalize-space($siren)" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$missingMarker" />
                    </xsl:otherwise>
                </xsl:choose>
            </cbc:ID>
        </cac:PartyIdentification>
    </xsl:template>

    <!-- Create CompanyID with SIREN (schemeID=0002)
         Validates length = 9 characters -->
    <xsl:template name="ubl:company-siren">
        <xsl:param name="siren" as="xs:string?" />
        <xsl:param name="missingMarker" as="xs:string" select="'**MISSING_SIREN**'" />
        <xsl:param name="invalidMarker" as="xs:string" select="'**INVALID_SIREN_LENGTH**'" />
        
        <cbc:CompanyID schemeID="0002">
            <xsl:choose>
                <xsl:when test="normalize-space($siren) != '' and string-length(normalize-space($siren)) = 9">
                    <xsl:value-of select="normalize-space($siren)" />
                </xsl:when>
                <xsl:when test="normalize-space($siren) != ''">
                    <xsl:value-of select="$invalidMarker" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$missingMarker" />
                </xsl:otherwise>
            </xsl:choose>
        </cbc:CompanyID>
    </xsl:template>

    <!-- Create PartyIdentification with SIRET (schemeID=0009) -->
    <xsl:template name="ubl:party-siret">
        <xsl:param name="siret" as="xs:string?" />
        <xsl:param name="missingMarker" as="xs:string" select="'**MISSING_SIRET**'" />
        
        <cac:PartyIdentification>
            <cbc:ID schemeID="0009">
                <xsl:choose>
                    <xsl:when test="normalize-space($siret) != ''">
                        <xsl:value-of select="normalize-space($siret)" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$missingMarker" />
                    </xsl:otherwise>
                </xsl:choose>
            </cbc:ID>
        </cac:PartyIdentification>
    </xsl:template>

    <!-- Create PartyIdentification with GLN (schemeID=0088) -->
    <xsl:template name="ubl:party-gln">
        <xsl:param name="gln" as="xs:string?" />
        <xsl:param name="missingMarker" as="xs:string" select="'**MISSING_GLN**'" />
        
        <cac:PartyIdentification>
            <cbc:ID schemeID="0088">
                <xsl:choose>
                    <xsl:when test="normalize-space($gln) != ''">
                        <xsl:value-of select="normalize-space($gln)" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$missingMarker" />
                    </xsl:otherwise>
                </xsl:choose>
            </cbc:ID>
        </cac:PartyIdentification>
    </xsl:template>

    <!-- Create CompanyID with VAT number
         Validates FR prefix for French VAT -->
    <xsl:template name="ubl:company-vat">
        <xsl:param name="vat" as="xs:string?" />
        <xsl:param name="missingMarker" as="xs:string" select="'**MISSING_VAT**'" />
        <xsl:param name="invalidMarker" as="xs:string" select="'**INVALID_VAT_FORMAT**'" />
        
        <cbc:CompanyID>
            <xsl:choose>
                <xsl:when test="normalize-space($vat) != '' and starts-with(normalize-space($vat), 'FR')">
                    <xsl:value-of select="normalize-space($vat)" />
                </xsl:when>
                <xsl:when test="normalize-space($vat) != ''">
                    <xsl:value-of select="$invalidMarker" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$missingMarker" />
                </xsl:otherwise>
            </xsl:choose>
        </cbc:CompanyID>
    </xsl:template>

    <!-- Create PartyTaxScheme with VAT -->
    <xsl:template name="ubl:party-tax-vat">
        <xsl:param name="vat" as="xs:string?" />
        <xsl:param name="missingMarker" as="xs:string" select="'**MISSING_VAT**'" />
        <xsl:param name="invalidMarker" as="xs:string" select="'**INVALID_VAT_FORMAT**'" />
        
        <cac:PartyTaxScheme>
            <cbc:CompanyID>
                <xsl:choose>
                    <xsl:when test="normalize-space($vat) != '' and starts-with(normalize-space($vat), 'FR')">
                        <xsl:value-of select="normalize-space($vat)" />
                    </xsl:when>
                    <xsl:when test="normalize-space($vat) != ''">
                        <xsl:value-of select="$invalidMarker" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$missingMarker" />
                    </xsl:otherwise>
                </xsl:choose>
            </cbc:CompanyID>
            <cac:TaxScheme>
                <cbc:ID>VAT</cbc:ID>
            </cac:TaxScheme>
        </cac:PartyTaxScheme>
    </xsl:template>

    <!-- Create Endpoint ID with default scheme -->
    <xsl:template name="ubl:endpoint-id">
        <xsl:param name="id" as="xs:string?" />
        <xsl:param name="schemeID" as="xs:string" select="'0225'" />
        <xsl:param name="missingMarker" as="xs:string" select="'**MISSING_ENDPOINT**'" />
        
        <cbc:EndpointID schemeID="{$schemeID}">
            <xsl:choose>
                <xsl:when test="normalize-space($id) != ''">
                    <xsl:value-of select="normalize-space($id)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$missingMarker" />
                </xsl:otherwise>
            </xsl:choose>
        </cbc:EndpointID>
    </xsl:template>

    <!-- Create Country with default FR -->
    <xsl:template name="ubl:country">
        <xsl:param name="code" as="xs:string?" />
        <xsl:param name="default" as="xs:string" select="'FR'" />
        
        <xsl:variable name="normalizedCode" select="normalize-space($code)" />
        
        <cac:Country>
            <cbc:IdentificationCode>
                <xsl:choose>
                    <xsl:when test="$normalizedCode != ''">
                        <xsl:choose>
                            <xsl:when test="upper-case($normalizedCode) = 'FRANCE'">FR</xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$normalizedCode" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$default" />
                    </xsl:otherwise>
                </xsl:choose>
            </cbc:IdentificationCode>
        </cac:Country>
    </xsl:template>

    <!-- Create InvoiceTypeCode with default 380 (Commercial Invoice) -->
    <xsl:template name="ubl:invoice-type-code">
        <xsl:param name="code" as="xs:string?" />
        <xsl:param name="default" as="xs:string" select="'380'" />
        
        <cbc:InvoiceTypeCode>
            <xsl:choose>
                <xsl:when test="normalize-space($code) != ''">
                    <xsl:value-of select="normalize-space($code)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$default" />
                </xsl:otherwise>
            </xsl:choose>
        </cbc:InvoiceTypeCode>
    </xsl:template>

    <!-- Create DocumentCurrencyCode with default currency code -->
    <xsl:template name="ubl:currency-code">
        <xsl:param name="code" as="xs:string?" />
        <xsl:param name="default" as="xs:string" select="$defaultCurrency" />
        
        <cbc:DocumentCurrencyCode>
            <xsl:choose>
                <xsl:when test="normalize-space($code) != ''">
                    <xsl:value-of select="normalize-space($code)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$default" />
                </xsl:otherwise>
            </xsl:choose>
        </cbc:DocumentCurrencyCode>
    </xsl:template>

</xsl:stylesheet>
