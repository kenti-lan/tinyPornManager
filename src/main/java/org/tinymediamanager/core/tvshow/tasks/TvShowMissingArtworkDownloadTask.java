package org.tinymediamanager.core.tvshow.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.TvShowArtworkHelper;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.exceptions.UnsupportedMediaTypeException;
import org.tinymediamanager.scraper.mediaprovider.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.mediaprovider.ITvShowMetadataProvider;
import org.tinymediamanager.ui.UTF8Control;

/**
 * the class TvShowMissingArtworkDownloadTask is used to download missing artwork for TV shows
 */
public class TvShowMissingArtworkDownloadTask extends TmmThreadPool {
  private static final Logger                LOGGER = LoggerFactory.getLogger(TvShowMissingArtworkDownloadTask.class);
  private static final ResourceBundle        BUNDLE = ResourceBundle.getBundle("messages", new UTF8Control());        //$NON-NLS-1$

  private final List<TvShow>                 tvShows;
  private final List<TvShowEpisode>          episodes;
  private final TvShowSearchAndScrapeOptions scrapeOptions;

  public TvShowMissingArtworkDownloadTask(List<TvShow> tvShows, List<TvShowEpisode> episodes, TvShowSearchAndScrapeOptions scrapeOptions) {
    super(BUNDLE.getString("task.missingartwork"));
    this.tvShows = new ArrayList<>(tvShows);
    this.episodes = new ArrayList<>(episodes);
    this.scrapeOptions = scrapeOptions;

    // add the episodes from the shows
    for (TvShow show : this.tvShows) {
      for (TvShowEpisode episode : new ArrayList<>(show.getEpisodes())) {
        if (!this.episodes.contains(episode)) {
          this.episodes.add(episode);
        }
      }
    }
  }

  @Override
  protected void doInBackground() {
    LOGGER.info("Getting missing artwork");

    initThreadPool(3, "scrapeMissingMovieArtwork");
    start();

    for (TvShow show : tvShows) {
      if (cancel) {
        break;
      }
      if (TvShowArtworkHelper.hasMissingArtwork(show, scrapeOptions.getTvShowScraperMetadataConfig())) {
        submitTask(new TvShowWorker(show));
      }
    }

    for (TvShowEpisode episode : episodes) {
      if (cancel) {
        break;
      }
      if (TvShowArtworkHelper.hasMissingArtwork(episode, scrapeOptions.getTvShowEpisodeScraperMetadataConfig())) {
        submitTask(new TvShowEpisodeWorker(episode));
      }
    }

    waitForCompletionOrCancel();
    LOGGER.info("Done getting missing artwork");
  }

  @Override
  public void callback(Object obj) {
    publishState((String) obj, progressDone);
  }

  /****************************************************************************************
   * Helper classes
   ****************************************************************************************/
  private static class TvShowWorker implements Runnable {
    private TvShowList tvShowList = TvShowList.getInstance();
    private TvShow     tvShow;

    private TvShowWorker(TvShow tvShow) {
      this.tvShow = tvShow;
    }

    @Override
    public void run() {
      try {
        // set up scrapers
        List<MediaArtwork> artwork = new ArrayList<>();
        MediaScrapeOptions options = new MediaScrapeOptions(MediaType.TV_SHOW);
        options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
        options.setLanguage(TvShowModuleManager.SETTINGS.getScraperLanguage().toLocale());
        options.setCountry(TvShowModuleManager.SETTINGS.getCertificationCountry());
        for (Map.Entry<String, Object> entry : tvShow.getIds().entrySet()) {
          options.setId(entry.getKey(), entry.getValue().toString());
        }

        // scrape providers till one artwork has been found
        for (MediaScraper artworkScraper : tvShowList.getDefaultArtworkScrapers()) {
          ITvShowArtworkProvider artworkProvider = (ITvShowArtworkProvider) artworkScraper.getMediaProvider();
          try {
            artwork.addAll(artworkProvider.getArtwork(options));
          }
          catch (ScrapeException e) {
            LOGGER.error("getArtwork", e);
            MessageManager.instance.pushMessage(
                new Message(Message.MessageLevel.ERROR, tvShow, "message.scrape.tvshowartworkfailed", new String[] { ":", e.getLocalizedMessage() }));
          }
          catch (MissingIdException ignored) {
            LOGGER.debug("no id found for scraper {}", artworkProvider.getProviderInfo());
          }
        }

        // now set & download the artwork
        if (!artwork.isEmpty()) {
          TvShowArtworkHelper.downloadMissingArtwork(tvShow, artwork);
        }
      }
      catch (Exception e) {
        LOGGER.error("Thread crashed", e);
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowMissingArtwork", "message.scrape.threadcrashed",
            new String[] { ":", e.getLocalizedMessage() }));
      }
    }
  }

  private static class TvShowEpisodeWorker implements Runnable {
    private TvShowList    tvShowList = TvShowList.getInstance();
    private TvShowEpisode episode;

    private TvShowEpisodeWorker(TvShowEpisode episode) {
      this.episode = episode;
    }

    @Override
    public void run() {
      try {
        // set up scrapers
        MediaScrapeOptions options = new MediaScrapeOptions(MediaType.TV_EPISODE);

        options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
        options.setLanguage(TvShowModuleManager.SETTINGS.getScraperLanguage().toLocale());
        options.setCountry(TvShowModuleManager.SETTINGS.getCertificationCountry());
        for (Map.Entry<String, Object> entry : episode.getTvShow().getIds().entrySet()) {
          options.setId(entry.getKey(), entry.getValue().toString());
        }
        if (episode.isDvdOrder()) {
          options.setId(MediaMetadata.SEASON_NR_DVD, String.valueOf(episode.getDvdSeason()));
          options.setId(MediaMetadata.EPISODE_NR_DVD, String.valueOf(episode.getDvdEpisode()));
        }
        else {
          options.setId(MediaMetadata.SEASON_NR, String.valueOf(episode.getAiredSeason()));
          options.setId(MediaMetadata.EPISODE_NR, String.valueOf(episode.getAiredEpisode()));
        }
        options.setArtworkType(MediaArtwork.MediaArtworkType.THUMB);

        // episode artwork is only provided by the meta data provider (not artwork provider)
        MediaMetadata metadata = ((ITvShowMetadataProvider) tvShowList.getDefaultMediaScraper().getMediaProvider()).getMetadata(options);
        List<MediaArtwork> mas = metadata.getMediaArt(MediaArtwork.MediaArtworkType.THUMB);
        if (!mas.isEmpty()) {
          episode.setArtworkUrl(mas.get(0).getDefaultUrl(), MediaFileType.THUMB);
          episode.writeThumbImage();
        }
      }
      catch (ScrapeException e) {
        LOGGER.error("getArtwork", e);
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowMissingArtwork", "message.scrape.threadcrashed",
            new String[] { ":", e.getLocalizedMessage() }));
      }
      catch (MissingIdException e) {
        LOGGER.warn("missing id for scrape");
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, episode, "scraper.error.missingid"));
      }
      catch (UnsupportedMediaTypeException | NothingFoundException ignored) {
      }
    }
  }
}
