package org.alekseev.util; // Пакет: сюда кладём утилиты/инфраструктуру (не бизнес-логику)

import org.hibernate.SessionFactory; // SessionFactory — “фабрика” для создания Session (тяжёлый объект)
import org.hibernate.cfg.Configuration; // Configuration читает hibernate.cfg.xml и строит SessionFactory

/**
 * HibernateUtil — класс-утилита, который создаёт SessionFactory один раз на всё приложение
 * и отдаёт его всем остальным частям (DAO/Service/UI).
 */
public final class HibernateUtil { // final: запрещаем наследование (так обычно делают для util-классов)

    // volatile: гарантирует корректную видимость переменной между потоками (важно для ленивой инициализации)
    private static volatile SessionFactory sessionFactory; // static: один объект на весь класс (на весь процесс JVM)

    private HibernateUtil() { // приватный конструктор: запрещаем new HibernateUtil()
        // Если бы конструктор был public, кто-то мог бы создать экземпляр утилиты — это нам не нужно
    }

    /**
     * Возвращает SessionFactory.
     * Создаёт его при первом вызове, дальше возвращает уже созданный экземпляр.
     */
    public static SessionFactory getSessionFactory() { // public: доступен из любого места (DAO/Service/UI)

        if (sessionFactory == null) {

            // synchronized: блокируем доступ, чтобы два потока не создали фабрику одновременно
            synchronized (HibernateUtil.class) { // лочим именно объект класса HibernateUtil

                // вторая проверка нужна из-за “double-check locking”:
                // пока один поток ждал lock, другой мог уже создать sessionFactory
                if (sessionFactory == null) { // если всё ещё null — значит создаём прямо сейчас

                    // Configuration() — объект, который читает настройки Hibernate и Entity-маппинги
                    sessionFactory = new Configuration()
                            .configure() // читает hibernate.cfg.xml из classpath (src/main/resources)
                            .buildSessionFactory(); // строит SessionFactory: тяжёлая операция

                    // Shutdown hook — код, который выполнится при завершении JVM (закрытие программы)
                    // Это страховка, чтобы фабрика закрылась даже если приложение завершилось “не идеально”
                    Runtime.getRuntime().addShutdownHook( // получаем объект Runtime текущей JVM и добавляем “крючок”
                            new Thread(HibernateUtil::shutdown) // создаём поток, который вызовет shutdown()
                    );
                }
            }
        }

        // Возвращаем уже созданный (или только что созданный) SessionFactory
        return sessionFactory;
    }

    /**
     * Корректно закрывает SessionFactory и освобождает ресурсы (соединения, пулы и т.п.).
     * Это нужно вызывать при завершении приложения (или положиться на shutdown hook).
     */
    public static void shutdown() { // public: чтобы можно было закрыть фабрику из App (main)

        // Проверка: фабрика существует и не закрыта
        if (sessionFactory != null && !sessionFactory.isClosed()) { // isClosed() защищает от повторного close()
            sessionFactory.close(); // закрываем фабрику (Hibernate закрывает пул/ресурсы)
        }
    }
}
