package com.relogging.server.service.crawling

interface CrawlingService {
    fun crawlAndSave(): Int
    fun crawlAndSaveNewsArticles(): Int
}
