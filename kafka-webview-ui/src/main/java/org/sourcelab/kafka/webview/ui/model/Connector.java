package org.sourcelab.kafka.webview.ui.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@AllArgsConstructor
public class Connector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String plugin;

    @Column(nullable = false)
    private int task;

    @ManyToOne(fetch = FetchType.LAZY)
    private Cluster cluster;
}
