package app.sintoniza.media

import android.service.notification.NotificationListenerService

/**
 * Vazio de propósito. O Android só entrega as sessões de mídia ativas
 * (MediaSessionManager.getActiveSessions) a apps com acesso a notificações;
 * este serviço existe para receber esse acesso. Nenhuma notificação é lida.
 */
class MediaAccessService : NotificationListenerService()
