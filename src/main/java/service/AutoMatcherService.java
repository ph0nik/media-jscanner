package service;

import model.DeductedQuery;

import java.util.concurrent.Future;

public interface AutoMatcherService {

    // take movie file name
    // find four numbers and extract phrase before them
    // use phrase as search query with numbers as year
    // perform tmdb search and get results
    // if list contains only one element apply this single result

    // test
    // Quantum of Solace [2008].MULTi.2160p.UHD.BluRay.REMUX.HEVC.DTS-HD.MA.5.1-presa
    // 1917.2019.MULTi.2160p.UHD.BluRay.REMUX.HDR10+.HEVC.TrueHD.ATMOS.7.1
    DeductedQuery extractTitleAndYear(String path);

    void autoMatchFiles();

    Future<Boolean> autoMatchFilesWithFuture();


        // regex - "\b^.+\d{4}\b"

}
