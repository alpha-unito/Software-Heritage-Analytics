# Copyright 2013-2023 Lawrence Livermore National Security, LLC and other
# Spack Project Developers. See the top-level COPYRIGHT file for details.
#
# SPDX-License-Identifier: (Apache-2.0 OR MIT)

# ----------------------------------------------------------------------------
# If you submit this package back to Spack as a pull request,
# please first remove this boilerplate and all FIXME comments.
#
# This is a template package file for Spack.  We've put "FIXME"
# next to all the things you'll want to change. Once you've handled
# them, you can save this file and test your package like this:
#
#     spack install sha
#
# You can edit this file again by typing:
#
#     spack edit sha
#
# See the Spack documentation for more information on packaging.
# ----------------------------------------------------------------------------

import os
from spack.package import *


class Sha(Package):
    """FIXME: Put a proper description of your package here."""

    # FIXME: Add a proper url for your package's homepage here.
    # homepage = "https://www.example.com"
    git = "https://github.com/alpha-unito/Software-Heritage-Analytics"
    url = "https://github.com/alpha-unito/Software-Heritage-Analytics/raw/main/download/Sha_Package_Spack.tar.gz"

#    version(
 #       "1.2.4", sha256="44722fe396d797a2f3e9b7a28bb59503de9591c76843e992c5a9ceebf1eb48bf")
    version(
        "1.2.4", sha256="e14dff30b18e1e9aa7eb9e97ad284451ded1f9e1ebfffe780a10e72cf964735d")
    depends_on("python", type='run')
    depends_on('py-requests', type='run')
    depends_on('py-numpy', type='run')
    depends_on(
        'spark_332_bin_hadoop3_scala213@3.3.2-bin-hadoop3-scala2.13', type='run')
    depends_on("openjdk@11.0.17_8", type='run')

    def setup_run_environment(self, env):
        """Add 'bin' to PATH."""
        env.prepend_path("PATH", join_path(self.prefix, 'Orchestrator', 'bin'))
        env.prepend_path("PATH", join_path(self.prefix, 'Cachemire', 'bin'))
        env.set("ORCHESTRATOR_HOME", join_path(
            self.prefix, 'Orchestrator'))

    def install(self, spec, prefix):
        """ Make objs and libs directories """

        mkdirp(join_path('Cachemire', 'objs'))

        mkdirp(join_path('Cachemire', 'libs'))

        mkdirp(join_path('Cachemire', 'bin'))

        """ Move to Cachemire dir and buid cachemire"""
        with working_dir(join_path('Cachemire')):
            make()

        """ Copy all dirs """

        install_tree(join_path('Cachemire', 'bin'),
                     join_path(prefix, 'Cachemire', 'bin'))

        install_tree(join_path('Orchestrator', 'src'),
                     join_path(prefix, 'Orchestrator', 'src'))

        mkdirp(join_path(prefix, 'Orchestrator', 'bin'))

        target = os.path.join(prefix, 'Orchestrator', 'src', 'pycachemire.py')
        link_name = os.path.join(prefix, 'Orchestrator', 'bin', 'orchestrator')
        os.symlink(target, link_name)

        target = os.path.join(prefix, 'Orchestrator',
                              'src', 'dashboardclient.py')
        link_name = os.path.join(
            prefix, 'Orchestrator', 'bin', 'dashboardclient')
        os.symlink(target, link_name)
