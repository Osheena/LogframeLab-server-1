package com.arqaam.logframelab.model.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Entity(name = "Indicator")
@Table(name = "IND_INDICATOR")
@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class Indicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME", length = 350)
    private String name;

    @Column(name = "DESCRIPTION", length = 400)
    private String description;

    @Column(name = "KEYWORDS", length = 350)
    private String keywords;

    @OneToOne
    @JoinColumn(name="Level")
    private Level level;

    @Column(name = "THEMES")
    private String themes;

    @ManyToMany
    @JoinTable(name = "INDICATOR_SOURCE")
    private Set<Source> source;

    @Column(name = "DISAGGREGATION")
    private Boolean disaggregation;

    @ManyToMany
    @JoinTable(
            name = "INDICATOR_CRS_CODE",
            inverseJoinColumns = @JoinColumn(name = "CRS_CODE_ID")
    )
    private Set<CRSCode> crsCode;

    @ManyToMany
    @JoinTable(
        name = "INDICATOR_SDG_CODE",
        inverseJoinColumns = @JoinColumn(name = "SDG_CODE_ID")
    )
    private Set<SDGCode> sdgCode;

    @Column(name = "SOURCE_VERIFICATION")
    private String sourceVerification;

    @Column(name = "DATA_SOURCE")
    private String dataSource;

    @Column(name = "TEMP")
    private boolean temp;

    @Transient
    private List<String> keywordsList;

    @Transient
    private String date;

    @Transient
    private String value;

    @Transient
    @Builder.Default
    private Integer numTimes = 1;

    public List<String> getKeywordsList() {
        if(keywordsList == null && keywords != null && !keywords.isEmpty()){
            keywordsList = new ArrayList<>(Arrays.asList(keywords.split(",")));
        }
        return keywordsList;
    }
}
