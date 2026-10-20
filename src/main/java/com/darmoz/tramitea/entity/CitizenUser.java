package com.darmoz.tramitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Perfil ciudadano de Tramitea. {@code id} es el id propio (al que apuntan
 * tareas, votos y comentarios). Según el modo de auth:
 * <ul>
 *   <li>local: también es la cuenta de login (password BCrypt) y el sub del token.</li>
 *   <li>darmoz: la cuenta vive en darmoz-auth; {@code idAuthUser} guarda el id de
 *   auth_users (sub de su token) y {@code passwordHash} queda null.</li>
 * </ul>
 */
@Entity
@Table(name = "tr_users")
public class CitizenUser {

    @Id
    private UUID id;

    /** Id del usuario en darmoz-auth (auth_users.id). Null en modo local. Relación lógica, sin FK. */
    @Column(name = "id_auth_user")
    private UUID idAuthUser;

    /** Siempre en minusculas: es el identificador de login. */
    @Column(nullable = false)
    private String email;

    /** Null en las cuentas de darmoz-auth. */
    @Column(name = "password_hash")
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

    /** Perfil de un usuario de darmoz-auth visto por primera vez: sin password ni nombres. */
    public static CitizenUser fromAuthUser(UUID idAuthUser, String email) {
        CitizenUser user = new CitizenUser(email, null, "", "");
        user.idAuthUser = idAuthUser;
        return user;
    }

    /** Vincula un perfil existente (p. ej. una cuenta local con el mismo email) a darmoz-auth. */
    public void linkAuthUser(UUID idAuthUser) {
        this.idAuthUser = idAuthUser;
        this.updatedAt = Instant.now();
    }

    /** El email de login lo administra darmoz-auth: se sincroniza desde su token. */
    public void syncEmail(String email) {
        this.email = email;
        this.updatedAt = Instant.now();
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
    public UUID getIdAuthUser() { return idAuthUser; }
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
