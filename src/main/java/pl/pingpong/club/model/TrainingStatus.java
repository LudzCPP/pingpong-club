package pl.pingpong.club.model;

public enum TrainingStatus {
    SCHEDULED,   // zaplanowany
    COMPLETED,   // zrealizowany – wchodzi do rozliczeń
    CANCELLED    // odwołany – nie nalicza opłaty
}
