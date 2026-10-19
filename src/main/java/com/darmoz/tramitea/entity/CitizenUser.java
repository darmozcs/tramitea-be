package com.darmoz.tramitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Cuenta y perfil ciudadano. El id es el sub del token de sesion. */
@Entity
@Table(name = "tr_users")
public class CitizenUser {

    @Id
    private UUID id;

    /** Siempre en minusculas: es el identificador de login. */
    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "national_id_verified", nullable = false)
    private boolean nationalIdVerified;

    private String phone;

    private UruguayDepartment department;

    private String locality;

    @Column(name = "notify_document_expiry", nullable = false)
    private boolean notifyDocumentExpiry = true;

    @Column(name = "notify_appointment_reminders", nullable = false)
    private boolean notifyAppointmentReminders = true;

    @Column(name = "notify_news_and_updates", nullable = false)
    private boolean notifyNewsAndUpdates;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CitizenUser() {
    }

    public CitizenUser(String email, String passwordHash, String firstName, String lastName) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updatePersonalData(String firstName, String lastName, String email, String phone,
                                   UruguayDepartment department, String locality) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.locality = locality;
        this.updatedAt = Instant.now();
    }

    public void updateNotificationPreferences(boolean documentExpiry, boolean appointmentReminders,
                                              boolean newsAndUpdates) {
        this.notifyDocumentExpiry = documentExpiry;
        this.notifyAppointmentReminders = appointmentReminders;
        this.notifyNewsAndUpdates = newsAndUpdates;
        this.updatedAt = Instant.now();
    }

    /** Nombre para mostrar en la comunidad. */
    public String getDisplayName() {
        return (firstName + " " + lastName).trim();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getNationalId() { return nationalId; }
    public boolean isNationalIdVerified() { return nationalIdVerified; }
    public String getPhone() { return phone; }
    public UruguayDepartment getDepartment() { return department; }
    public String getLocality() { return locality; }
    public boolean isNotifyDocumentExpiry() { return notifyDocumentExpiry; }
    public boolean isNotifyAppointmentReminders() { return notifyAppointmentReminders; }
    public boolean isNotifyNewsAndUpdates() { return notifyNewsAndUpdates; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
